/**
 * Copyright © 2016-2020 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.telemetry;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.thingsboard.server.cassandra.TsKvHourHistoryEntity;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.kv.*;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.util.TenantRateLimitException;
import org.thingsboard.server.service.security.AccessValidator;
import org.thingsboard.server.service.security.ValidationCallback;
import org.thingsboard.server.service.security.ValidationResult;
import org.thingsboard.server.service.security.ValidationResultCode;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.telemetry.cmd.*;
import org.thingsboard.server.service.telemetry.exception.UnauthorizedException;
import org.thingsboard.server.service.telemetry.sub.SubscriptionErrorCode;
import org.thingsboard.server.service.telemetry.sub.SubscriptionState;
import org.thingsboard.server.service.telemetry.sub.SubscriptionUpdate;
import org.thingsboard.server.utils.DateUtils;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.springframework.data.cassandra.core.query.Criteria.where;

/**
 * Created by ashvayka on 27.03.18.
 */
@Service
@Slf4j
public class DefaultTelemetryWebSocketService implements TelemetryWebSocketService {

    private static final int DEFAULT_LIMIT = 100;
    private static final Aggregation DEFAULT_AGGREGATION = Aggregation.NONE;
    private static final int UNKNOWN_SUBSCRIPTION_ID = 0;
    private static final String PROCESSING_MSG = "[{}] Processing: {}";
    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private static final String FAILED_TO_FETCH_DATA = "Failed to fetch data!";
    private static final String FAILED_TO_FETCH_ATTRIBUTES = "Failed to fetch attributes!";
    private static final String SESSION_META_DATA_NOT_FOUND = "Session meta-data not found!";


    private final ConcurrentMap<String, WsSessionMetaData> wsSessionsMap = new ConcurrentHashMap<>();

    @Autowired
    CassandraTemplate cassandraTemplate;

    @Autowired
    private TelemetrySubscriptionService subscriptionManager;

    @Autowired
    private TelemetryWebSocketMsgEndpoint msgEndpoint;

    @Autowired
    private AccessValidator accessValidator;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private TimeseriesService tsService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${server.ws.limits.max_subscriptions_per_tenant:0}")
    private int maxSubscriptionsPerTenant;
    @Value("${server.ws.limits.max_subscriptions_per_customer:0}")
    private int maxSubscriptionsPerCustomer;
    @Value("${server.ws.limits.max_subscriptions_per_regular_user:0}")
    private int maxSubscriptionsPerRegularUser;
    @Value("${server.ws.limits.max_subscriptions_per_public_user:0}")
    private int maxSubscriptionsPerPublicUser;

    private ConcurrentMap<TenantId, Set<String>> tenantSubscriptionsMap = new ConcurrentHashMap<>();
    private ConcurrentMap<CustomerId, Set<String>> customerSubscriptionsMap = new ConcurrentHashMap<>();
    private ConcurrentMap<UserId, Set<String>> regularUserSubscriptionsMap = new ConcurrentHashMap<>();
    private ConcurrentMap<UserId, Set<String>> publicUserSubscriptionsMap = new ConcurrentHashMap<>();

    private ExecutorService executor;

    @PostConstruct
    public void initExecutor() {
        executor = Executors.newWorkStealingPool(50);
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /***
     * socket连接事件
     * @param sessionRef
     * @param event
     */
    @Override
    public void handleWebSocketSessionEvent(TelemetryWebSocketSessionRef sessionRef, SessionEvent event) {
        String sessionId = sessionRef.getSessionId();

        log.debug(PROCESSING_MSG, sessionId, event);
        switch (event.getEventType()) {
            case ESTABLISHED:
                wsSessionsMap.put(sessionId, new WsSessionMetaData(sessionRef));
                break;
            case ERROR:
                log.debug("[{}] Unknown websocket session error: {}. ", sessionId, event.getError().orElse(null));
                break;
            case CLOSED:
                wsSessionsMap.remove(sessionId);
                subscriptionManager.cleanupLocalWsSessionSubscriptions(sessionRef, sessionId);
                processSessionClose(sessionRef);
                break;
        }
    }

    /***
     *  函数的回调
     *  前台传入的数据
     *  onmessage
     */
    @Override
    public void handleWebSocketMsg(TelemetryWebSocketSessionRef sessionRef, String msg) {


        if (log.isTraceEnabled()) {
            log.trace("[{}] Processing: {}", sessionRef.getSessionId(), msg);
        }
        try {
            TelemetryPluginCmdsWrapper cmdsWrapper = jsonMapper.readValue(msg, TelemetryPluginCmdsWrapper.class);
            if (cmdsWrapper != null) {
                if (cmdsWrapper.getAttrSubCmds() != null) {
                    cmdsWrapper.getAttrSubCmds().forEach(cmd -> {
                        if (processSubscription(sessionRef, cmd)) {
                            handleWsAttributesSubscriptionCmd(sessionRef, cmd);
                        }
                    });
                }
                /***
                 * 实时数据
                 */
                if (cmdsWrapper.getTsSubCmds() != null) {
                    cmdsWrapper.getTsSubCmds().forEach(cmd -> {
                        if (processSubscription(sessionRef, cmd)) {
                            handleWsTimeseriesSubscriptionCmd(sessionRef, cmd);
                        }
                    });

                }
                /***
                 *
                 */
                if (cmdsWrapper.getHistoryCmds() != null) {
                    cmdsWrapper.getHistoryCmds().forEach(cmd -> handleWsHistoryCmd(sessionRef, cmd));
                }
            }
        } catch (IOException e) {
            log.warn("Failed to decode subscription cmd: {}", e.getMessage(), e);
            SubscriptionUpdate update = new SubscriptionUpdate(UNKNOWN_SUBSCRIPTION_ID, SubscriptionErrorCode.INTERNAL_ERROR, SESSION_META_DATA_NOT_FOUND);
            sendWsMsg(sessionRef, update);
        }
    }

    /****
     * 发送信息
     * @param sessionId
     * @param update
     */
    @Override
    public void sendWsMsg(String sessionId, SubscriptionUpdate update) {
        WsSessionMetaData md = wsSessionsMap.get(sessionId);
        if (md != null) {
            sendWsMsg(md.getSessionRef(), update);
        }
    }

    private void processSessionClose(TelemetryWebSocketSessionRef sessionRef) {
        String sessionId = "[" + sessionRef.getSessionId() + "]";
        if (maxSubscriptionsPerTenant > 0) {
            Set<String> tenantSubscriptions = tenantSubscriptionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getTenantId(), id -> ConcurrentHashMap.newKeySet());
            synchronized (tenantSubscriptions) {
                tenantSubscriptions.removeIf(subId -> subId.startsWith(sessionId));
            }
        }
        if (sessionRef.getSecurityCtx().isCustomerUser()) {
            if (maxSubscriptionsPerCustomer > 0) {
                Set<String> customerSessions = customerSubscriptionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getCustomerId(), id -> ConcurrentHashMap.newKeySet());
                synchronized (customerSessions) {
                    customerSessions.removeIf(subId -> subId.startsWith(sessionId));
                }
            }
            if (maxSubscriptionsPerRegularUser > 0 && UserPrincipal.Type.USER_NAME.equals(sessionRef.getSecurityCtx().getUserPrincipal().getType())) {
                Set<String> regularUserSessions = regularUserSubscriptionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getId(), id -> ConcurrentHashMap.newKeySet());
                synchronized (regularUserSessions) {
                    regularUserSessions.removeIf(subId -> subId.startsWith(sessionId));
                }
            }
            if (maxSubscriptionsPerPublicUser > 0 && UserPrincipal.Type.PUBLIC_ID.equals(sessionRef.getSecurityCtx().getUserPrincipal().getType())) {
                Set<String> publicUserSessions = publicUserSubscriptionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getId(), id -> ConcurrentHashMap.newKeySet());
                synchronized (publicUserSessions) {
                    publicUserSessions.removeIf(subId -> subId.startsWith(sessionId));
                }
            }
        }
    }

    private boolean processSubscription(TelemetryWebSocketSessionRef sessionRef, SubscriptionCmd cmd) {
        String subId = "[" + sessionRef.getSessionId() + "]:[" + cmd.getCmdId() + "]";
        try {
            if (maxSubscriptionsPerTenant > 0) {
                Set<String> tenantSubscriptions = tenantSubscriptionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getTenantId(), id -> ConcurrentHashMap.newKeySet());
                synchronized (tenantSubscriptions) {
                    if (cmd.isUnsubscribe()) {
                        tenantSubscriptions.remove(subId);
                    } else if (tenantSubscriptions.size() < maxSubscriptionsPerTenant) {
                        tenantSubscriptions.add(subId);
                    } else {
                        log.info("[{}][{}][{}] Failed to start subscription. Max tenant subscriptions limit reached"
                                , sessionRef.getSecurityCtx().getTenantId(), sessionRef.getSecurityCtx().getId(), subId);
                        msgEndpoint.close(sessionRef, CloseStatus.POLICY_VIOLATION.withReason("Max tenant subscriptions limit reached!"));
                        return false;
                    }
                }
            }

            if (sessionRef.getSecurityCtx().isCustomerUser()) {
                if (maxSubscriptionsPerCustomer > 0) {
                    Set<String> customerSessions = customerSubscriptionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getCustomerId(), id -> ConcurrentHashMap.newKeySet());
                    synchronized (customerSessions) {
                        if (cmd.isUnsubscribe()) {
                            customerSessions.remove(subId);
                        } else if (customerSessions.size() < maxSubscriptionsPerCustomer) {
                            customerSessions.add(subId);
                        } else {
                            log.info("[{}][{}][{}] Failed to start subscription. Max customer subscriptions limit reached"
                                    , sessionRef.getSecurityCtx().getTenantId(), sessionRef.getSecurityCtx().getId(), subId);
                            msgEndpoint.close(sessionRef, CloseStatus.POLICY_VIOLATION.withReason("Max customer subscriptions limit reached"));
                            return false;
                        }
                    }
                }
                if (maxSubscriptionsPerRegularUser > 0 && UserPrincipal.Type.USER_NAME.equals(sessionRef.getSecurityCtx().getUserPrincipal().getType())) {
                    Set<String> regularUserSessions = regularUserSubscriptionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getId(), id -> ConcurrentHashMap.newKeySet());
                    synchronized (regularUserSessions) {
                        if (regularUserSessions.size() < maxSubscriptionsPerRegularUser) {
                            regularUserSessions.add(subId);
                        } else {
                            log.info("[{}][{}][{}] Failed to start subscription. Max regular user subscriptions limit reached"
                                    , sessionRef.getSecurityCtx().getTenantId(), sessionRef.getSecurityCtx().getId(), subId);
                            msgEndpoint.close(sessionRef, CloseStatus.POLICY_VIOLATION.withReason("Max regular user subscriptions limit reached"));
                            return false;
                        }
                    }
                }
                if (maxSubscriptionsPerPublicUser > 0 && UserPrincipal.Type.PUBLIC_ID.equals(sessionRef.getSecurityCtx().getUserPrincipal().getType())) {
                    Set<String> publicUserSessions = publicUserSubscriptionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getId(), id -> ConcurrentHashMap.newKeySet());
                    synchronized (publicUserSessions) {
                        if (publicUserSessions.size() < maxSubscriptionsPerPublicUser) {
                            publicUserSessions.add(subId);
                        } else {
                            log.info("[{}][{}][{}] Failed to start subscription. Max public user subscriptions limit reached"
                                    , sessionRef.getSecurityCtx().getTenantId(), sessionRef.getSecurityCtx().getId(), subId);
                            msgEndpoint.close(sessionRef, CloseStatus.POLICY_VIOLATION.withReason("Max public user subscriptions limit reached"));
                            return false;
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.warn("[{}] Failed to send session close: {}", sessionRef.getSessionId(), e);
            return false;
        }
        return true;
    }

    /**
     * @param sessionRef
     * @param cmd
     */
    private void handleWsAttributesSubscriptionCmd(TelemetryWebSocketSessionRef sessionRef, AttributesSubscriptionCmd cmd) {
        String sessionId = sessionRef.getSessionId();
        log.debug("[{}] Processing: {}", sessionId, cmd);

        if (validateSessionMetadata(sessionRef, cmd, sessionId)) {
            if (cmd.isUnsubscribe()) {
                unsubscribe(sessionRef, cmd, sessionId);
            } else if (validateSubscriptionCmd(sessionRef, cmd)) {
                EntityId entityId = EntityIdFactory.getByTypeAndId(cmd.getEntityType(), cmd.getEntityId());
                log.debug("[{}] fetching latest attributes ({}) values for device: {}", sessionId, cmd.getKeys(), entityId);
                Optional<Set<String>> keysOptional = getKeys(cmd);
                if (keysOptional.isPresent()) {
                    List<String> keys = new ArrayList<>(keysOptional.get());
                    handleWsAttributesSubscriptionByKeys(sessionRef, cmd, sessionId, entityId, keys);
                } else {
                    handleWsAttributesSubscription(sessionRef, cmd, sessionId, entityId);
                }
            }
        }
    }

    private void handleWsAttributesSubscriptionByKeys(TelemetryWebSocketSessionRef sessionRef,
                                                      AttributesSubscriptionCmd cmd, String sessionId, EntityId entityId,
                                                      List<String> keys) {
        FutureCallback<List<AttributeKvEntry>> callback = new FutureCallback<List<AttributeKvEntry>>() {
            @Override
            public void onSuccess(List<AttributeKvEntry> data) {
                List<TsKvEntry> attributesData = data.stream().map(d -> new BasicTsKvEntry(d.getLastUpdateTs(), d)).collect(Collectors.toList());

                sendWsMsg(sessionRef, new SubscriptionUpdate(cmd.getCmdId(), attributesData));

                Map<String, Long> subState = new HashMap<>(keys.size());
                keys.forEach(key -> subState.put(key, 0L));
                attributesData.forEach(v -> subState.put(v.getKey(), v.getTs()));

                SubscriptionState sub = new SubscriptionState(sessionId, cmd.getCmdId(), sessionRef.getSecurityCtx().getTenantId(), entityId, TelemetryFeature.ATTRIBUTES, false, subState, cmd.getScope());
                subscriptionManager.addLocalWsSubscription(sessionId, entityId, sub);
            }

            @Override
            public void onFailure(Throwable e) {
                log.error(FAILED_TO_FETCH_ATTRIBUTES, e);
                SubscriptionUpdate update;
                if (e instanceof UnauthorizedException) {
                    update = new SubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.UNAUTHORIZED,
                            SubscriptionErrorCode.UNAUTHORIZED.getDefaultMsg());
                } else {
                    update = new SubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR,
                            FAILED_TO_FETCH_ATTRIBUTES);
                }
                sendWsMsg(sessionRef, update);
            }
        };

        if (StringUtils.isEmpty(cmd.getScope())) {
            accessValidator.validate(sessionRef.getSecurityCtx(), Operation.READ_ATTRIBUTES, entityId, getAttributesFetchCallback(sessionRef.getSecurityCtx().getTenantId(), entityId, keys, callback));
        } else {
            accessValidator.validate(sessionRef.getSecurityCtx(), Operation.READ_ATTRIBUTES, entityId, getAttributesFetchCallback(sessionRef.getSecurityCtx().getTenantId(), entityId, cmd.getScope(), keys, callback));
        }
    }

    /***
     * 历史区间查询
     * @param sessionRef
     * @param cmd
     */
    private void handleWsHistoryCmd(TelemetryWebSocketSessionRef sessionRef, GetHistoryCmd cmd) {
        String sessionId = sessionRef.getSessionId();
        WsSessionMetaData sessionMD = wsSessionsMap.get(sessionId);
        if (sessionMD == null) {
            log.warn("[{}] Session meta data not found. ", sessionId);
            SubscriptionUpdate update = new SubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR,
                    SESSION_META_DATA_NOT_FOUND);
            sendWsMsg(sessionRef, update);
            return;
        }
        if (cmd.getEntityId() == null || cmd.getEntityId().isEmpty() || cmd.getEntityType() == null || cmd.getEntityType().isEmpty()) {
            SubscriptionUpdate update = new SubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.BAD_REQUEST,
                    "Device id is empty!");
            sendWsMsg(sessionRef, update);
            return;
        }
        if (cmd.getKeys() == null || cmd.getKeys().isEmpty()) {
            SubscriptionUpdate update = new SubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.BAD_REQUEST,
                    "Keys are empty!");
            sendWsMsg(sessionRef, update);
            return;
        }


        EntityId entityId = EntityIdFactory.getByTypeAndId(cmd.getEntityType(), cmd.getEntityId());
        List<String> keys = new ArrayList<>(getKeys(cmd).orElse(Collections.emptySet()));


        // 回调函数....
        FutureCallback<List<TsKvEntry>> callback = new FutureCallback<List<TsKvEntry>>() {
            /***
             * 加入一条警戒线的数据
             * @param data
             */
            @Override
            public void onSuccess(List<TsKvEntry> data) {
                /***
                 * 警戒线设备
                 */
                if (data.size() == 0) {
                    if ("48469ad0-2d37-11eb-8707-1323b5852caa".equals(cmd.getEntityId())) {
                        sendWsMsg(sessionRef, new SubscriptionUpdate(cmd.getCmdId(), warnList()));
                    } else {
                        System.out.println("历史区域查询");
                        sendWsMsg(sessionRef, new SubscriptionUpdate(cmd.getCmdId(), newList()));
                    }
                } else {
                    sendWsMsg(sessionRef, new SubscriptionUpdate(cmd.getCmdId(), data));
                }
            }

            @Override
            public void onFailure(Throwable e) {
                SubscriptionUpdate update;
                if (UnauthorizedException.class.isInstance(e)) {
                    update = new SubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.UNAUTHORIZED,
                            SubscriptionErrorCode.UNAUTHORIZED.getDefaultMsg());
                } else {
                    update = new SubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR,
                            FAILED_TO_FETCH_DATA);
                }
//                sendWsMsg(sessionRef, update);
            }

            // 获取到小时为单位的数据
            private List<TsKvEntry> newList() {
                List<TsKvEntry> entryList = new LinkedList<>();
                List<TsKvEntry> linkedList = new LinkedList<>();
                List<TsKvHourHistoryEntity> list = findAll();

                for (int i = 0; i < list.size(); i++) {
                    int finalI = i;
                    List<TsKvHourHistoryEntity> finalList = list;
                    TsKvEntry tsKvEntry = new TsKvEntry() {

                        // 鼠标的显示
                        @Override
                        public long getTs() {
                            return (finalList.get(finalI).getTs());
                        }

                        // 类型的显示
                        @Override
                        public String getKey() {
                            return (finalList.get(finalI).getKey());
                        }

                        @Override
                        public DataType getDataType() {
                            return DataType.STRING;
                        }

                        @Override
                        public Optional<String> getStrValue() {
                            return Optional.ofNullable(String.valueOf(finalList.get(finalI).getTs()));
                        }

                        @Override
                        public Optional<Long> getLongValue() {
                            return Optional.ofNullable(finalList.get(finalI).getTs());
                        }

                        @Override
                        public Optional<Boolean> getBooleanValue() {
                            return Optional.ofNullable(true);
                        }

                        @Override
                        public Optional<Double> getDoubleValue() {
                            return Optional.empty();
                        }

                        @Override
                        public String getValueAsString() {
                            return String.valueOf(finalList.get(finalI).getValue());
                        }

                        @Override
                        public Object getValue() {
                            return "TEST";
                        }
                    };
                    entryList.add(tsKvEntry);
                }
                /***
                 * 此处存在一个问题 如果后台返回的数据太大的话前台的渲染会很困难
                 */
                Long ts = cmd.getEndTs() - cmd.getStartTs();
                if (ts > DateUtils.getMonth()) {
                    Integer l = Math.toIntExact((ts / DateUtils.getMonth())) * 3;
                    Map<String, List<TsKvEntry>> listMap = entryList.stream().collect(Collectors.groupingBy(TsKvEntry::getKey));
                    for (String s : listMap.keySet()) {
                        List<TsKvEntry> kvEntryList = listMap.get(s);
                        for (int i = 0; i < kvEntryList.size(); i++) {
                            if (i % l == 0) {
                                linkedList.add(kvEntryList.get(i));
                            }
                        }

                    }
                    entryList.clear();
                }
                if (linkedList.size() == 0) {
                    return entryList;
                } else {

                    return linkedList;
                }


            }


            // 获取到警戒线的数据
            private List<TsKvEntry> warnList() {

                List<TsKvEntry> warnList = new LinkedList<>();

                for (long i = cmd.getStartTs(); i < cmd.getEndTs(); i += 36000) {

                    long finalI = i;
                    /***
                     *PM2.5
                     */
                    TsKvEntry t1 = new TsKvEntry() {

                        // 鼠标的显示
                        @Override
                        public long getTs() {
                            return (finalI);
                        }

                        // 类型的显示
                        @Override
                        public String getKey() {
                            return ("PM2.5");
                        }

                        @Override
                        public DataType getDataType() {
                            return DataType.STRING;
                        }

                        @Override
                        public Optional<String> getStrValue() {
                            return Optional.of(String.valueOf(75));
                        }

                        @Override
                        public Optional<Long> getLongValue() {
                            return Optional.ofNullable(cmd.getStartTs());
                        }

                        @Override
                        public Optional<Boolean> getBooleanValue() {
                            return Optional.ofNullable(true);
                        }

                        @Override
                        public Optional<Double> getDoubleValue() {
                            return Optional.empty();
                        }

                        @Override
                        public String getValueAsString() {
                            return "75";
                        }

                        @Override
                        public Object getValue() {
                            return "TEST";
                        }
                    };
                    warnList.add(t1);

                    /***
                     * pm10*/

                    TsKvEntry t2 = new TsKvEntry() {

                        // 鼠标的显示
                        @Override
                        public long getTs() {
                            return (finalI);
                        }

                        // 类型的显示
                        @Override
                        public String getKey() {
                            return ("PM10");
                        }

                        @Override
                        public DataType getDataType() {
                            return DataType.STRING;
                        }

                        @Override
                        public Optional<String> getStrValue() {
                            return Optional.of(String.valueOf(75));
                        }

                        @Override
                        public Optional<Long> getLongValue() {
                            return Optional.ofNullable(finalI);
                        }

                        @Override
                        public Optional<Boolean> getBooleanValue() {
                            return Optional.ofNullable(true);
                        }

                        @Override
                        public Optional<Double> getDoubleValue() {
                            return Optional.empty();
                        }

                        @Override
                        public String getValueAsString() {
                            return "75";
                        }

                        @Override
                        public Object getValue() {
                            return "TEST";
                        }
                    };
                    warnList.add(t2);

                    /****
                     * PM1.0
                     */
                    TsKvEntry t3 = new TsKvEntry() {

                        // 鼠标的显示
                        @Override
                        public long getTs() {
                            return (finalI);
                        }

                        // 类型的显示
                        @Override
                        public String getKey() {
                            return ("PM1.0");
                        }

                        @Override
                        public DataType getDataType() {
                            return DataType.STRING;
                        }

                        @Override
                        public Optional<String> getStrValue() {
                            return Optional.of(String.valueOf(75));
                        }

                        @Override
                        public Optional<Long> getLongValue() {
                            return Optional.ofNullable(finalI);
                        }

                        @Override
                        public Optional<Boolean> getBooleanValue() {
                            return Optional.ofNullable(true);
                        }

                        @Override
                        public Optional<Double> getDoubleValue() {
                            return Optional.empty();
                        }

                        @Override
                        public String getValueAsString() {
                            return "75";
                        }

                        @Override
                        public Object getValue() {
                            return "TEST";
                        }
                    };
                    warnList.add(t3);

                    /***
                     * CO2
                     */
                    TsKvEntry t4 = new TsKvEntry() {

                        // 鼠标的显示
                        @Override
                        public long getTs() {
                            return (finalI);
                        }

                        // 类型的显示
                        @Override
                        public String getKey() {
                            return ("CO2");
                        }

                        @Override
                        public DataType getDataType() {
                            return DataType.STRING;
                        }

                        @Override
                        public Optional<String> getStrValue() {
                            return Optional.of(String.valueOf(1000));
                        }

                        @Override
                        public Optional<Long> getLongValue() {
                            return Optional.ofNullable(finalI);
                        }

                        @Override
                        public Optional<Boolean> getBooleanValue() {
                            return Optional.ofNullable(true);
                        }

                        @Override
                        public Optional<Double> getDoubleValue() {
                            return Optional.empty();
                        }

                        @Override
                        public String getValueAsString() {
                            return "1000";
                        }

                        @Override
                        public Object getValue() {
                            return "TEST";
                        }
                    };
                    warnList.add(t4);

                    /***
                     * 甲醛
                     */
                    TsKvEntry t5 = new TsKvEntry() {

                        // 鼠标的显示
                        @Override
                        public long getTs() {
                            return (finalI);
                        }

                        // 类型的显示
                        @Override
                        public String getKey() {
                            return ("CH2O");
                        }

                        @Override
                        public DataType getDataType() {
                            return DataType.STRING;
                        }

                        @Override
                        public Optional<String> getStrValue() {
                            return Optional.of(String.valueOf(80));
                        }

                        @Override
                        public Optional<Long> getLongValue() {
                            return Optional.ofNullable(finalI);
                        }

                        @Override
                        public Optional<Boolean> getBooleanValue() {
                            return Optional.ofNullable(true);
                        }

                        @Override
                        public Optional<Double> getDoubleValue() {
                            return Optional.empty();
                        }

                        @Override
                        public String getValueAsString() {
                            return "80";
                        }

                        @Override
                        public Object getValue() {
                            return "TEST";
                        }
                    };
                    warnList.add(t5);
                }
                return warnList;

            }

            /**
             * redis
             * @return
             */
            private List<TsKvHourHistoryEntity> findAll() {
                List<TsKvHourHistoryEntity> list = new LinkedList<>();
                try {

                    Set<String> set = stringRedisTemplate.opsForZSet().rangeByScore(cmd.getEntityId(), cmd.getStartTs() - DateUtils.getDayTime(), cmd.getEndTs());
                    for (String s : set) {
                        List<TsKvHourHistoryEntity> array = JSON.parseArray(s, TsKvHourHistoryEntity.class);
                        list.addAll(array);
                    }
                    return list;
                } catch (Exception e) {
                    /***
                     * 强迫线程进行等待 异常后5秒继续
                     */
                    boolean flag = true;
                    while (flag) {
                        try {
                            Thread.currentThread().sleep(2000);
                            Set<String> set = stringRedisTemplate.opsForZSet().rangeByScore(cmd.getEntityId(), cmd.getStartTs() - DateUtils.getDayTime(), cmd.getEndTs());
                            for (String s : set) {
                                List<TsKvHourHistoryEntity> array = JSON.parseArray(s, TsKvHourHistoryEntity.class);
                                list.addAll(array);
                            }
                            flag = false;
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            flag = true;
                        }
                    }
                }

                System.out.println(list.size() + "集合的长度");
                return list;

            }


            /**
             * cassandra
             * @return
             */
            private List<TsKvHourHistoryEntity> findAllC() {
                List<TsKvHourHistoryEntity> list = new LinkedList<>();
                try {

                    list = cassandraTemplate.select(
                            Query.query(where("ts").gt(cmd.getStartTs())).and(where("ts").lt(cmd.getEndTs())).and(where("entity_id").is(cmd.getEntityId())).withAllowFiltering(), TsKvHourHistoryEntity.class);
                    return list;
                } catch (Exception e) {
                    /***
                     * 强迫线程进行等待 异常后5秒继续
                     */
                    boolean flag = true;
                    while (flag) {
                        try {
                            Thread.currentThread().sleep(20000);
                            list = cassandraTemplate.select(
                                    Query.query(where("ts").gt(cmd.getStartTs())).and(where("ts").lt(cmd.getEndTs())).and(where("entity_id").is(cmd.getEntityId())).withAllowFiltering(), TsKvHourHistoryEntity.class);

                            flag = false;
                        } catch (Exception ex) {
                            System.out.println("出错");
                            ex.printStackTrace();
                            flag = true;
                        }
                    }
                }
                return list;

            }

            ;
        };

        // 查询历史曲线的数据............
        if (cmd.getEndTs() - cmd.getStartTs() <= DateUtils.getDayTime() * 3) {
            List<ReadTsKvQuery> queries = keys.stream().map(key -> new BaseReadTsKvQuery(key, cmd.getStartTs(), cmd.getEndTs(), cmd.getInterval(), getLimit(cmd.getLimit()), getAggregation(cmd.getAgg())))
                    .collect(Collectors.toList());
            accessValidator.validate(sessionRef.getSecurityCtx(), Operation.READ_TELEMETRY, entityId,
                    on(r -> Futures.addCallback(tsService.findAll(sessionRef.getSecurityCtx().getTenantId(), entityId, queries), callback, executor), callback::onFailure));

        } else {

            long time = System.currentTimeMillis() + 1000000000;
            List<ReadTsKvQuery> queries = keys.stream().map(key -> new BaseReadTsKvQuery(key, time, time, cmd.getInterval(), getLimit(cmd.getLimit()), getAggregation(cmd.getAgg())))
                    .collect(Collectors.toList());

            accessValidator.validate(sessionRef.getSecurityCtx(), Operation.READ_TELEMETRY, entityId,
                    on(r -> Futures.addCallback(tsService.findAll(sessionRef.getSecurityCtx().getTenantId(), entityId, queries), callback, executor), callback::onFailure));
        }

    }


    /****
     * 222222
     * @param sessionRef
     * @param cmd
     * @param sessionId
     * @param entityId
     */
    private void handleWsAttributesSubscription(TelemetryWebSocketSessionRef sessionRef,
                                                AttributesSubscriptionCmd cmd, String sessionId, EntityId entityId) {

        FutureCallback<List<AttributeKvEntry>> callback = new FutureCallback<List<AttributeKvEntry>>() {
            @Override
            public void onSuccess(List<AttributeKvEntry> data) {
                List<TsKvEntry> attributesData = data.stream().map(d -> new BasicTsKvEntry(d.getLastUpdateTs(), d)).collect(Collectors.toList());
                sendWsMsg(sessionRef, new SubscriptionUpdate(cmd.getCmdId(), attributesData));

                Map<String, Long> subState = new HashMap<>(attributesData.size());
                attributesData.forEach(v -> subState.put(v.getKey(), v.getTs()));

                SubscriptionState sub = new SubscriptionState(sessionId, cmd.getCmdId(), sessionRef.getSecurityCtx().getTenantId(), entityId, TelemetryFeature.ATTRIBUTES, true, subState, cmd.getScope());
                subscriptionManager.addLocalWsSubscription(sessionId, entityId, sub);
            }

            @Override
            public void onFailure(Throwable e) {
                log.error(FAILED_TO_FETCH_ATTRIBUTES, e);
                SubscriptionUpdate update = new SubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR,
                        FAILED_TO_FETCH_ATTRIBUTES);
                sendWsMsg(sessionRef, update);
            }
        };


        if (StringUtils.isEmpty(cmd.getScope())) {
            accessValidator.validate(sessionRef.getSecurityCtx(), Operation.READ_ATTRIBUTES, entityId, getAttributesFetchCallback(sessionRef.getSecurityCtx().getTenantId(), entityId, callback));
        } else {
            accessValidator.validate(sessionRef.getSecurityCtx(), Operation.READ_ATTRIBUTES, entityId, getAttributesFetchCallback(sessionRef.getSecurityCtx().getTenantId(), entityId, cmd.getScope(), callback));
        }
    }


    /**
     * 实时数据查询
     *
     * @param sessionRef
     * @param cmd
     */
    private void handleWsTimeseriesSubscriptionCmd(TelemetryWebSocketSessionRef sessionRef, TimeseriesSubscriptionCmd cmd) {

        String sessionId = sessionRef.getSessionId();
        log.debug("[{}] Processing: {}", sessionId, cmd);

        if (validateSessionMetadata(sessionRef, cmd, sessionId)) {
            if (cmd.isUnsubscribe()) {
                unsubscribe(sessionRef, cmd, sessionId);
            } else if (validateSubscriptionCmd(sessionRef, cmd)) {
                EntityId entityId = EntityIdFactory.getByTypeAndId(cmd.getEntityType(), cmd.getEntityId());
                Optional<Set<String>> keysOptional = getKeys(cmd);
                if (keysOptional.isPresent()) {
                    handleWsTimeseriesSubscriptionByKeys(sessionRef, cmd, sessionId, entityId);
                } else {
                    handleWsTimeseriesSubscription(sessionRef, cmd, sessionId, entityId);
                }
            }
        }
    }

    /***
     * 预测实时的 接口
     *
     * @param sessionRef
     * @param cmd
     * @param sessionId
     * @param entityId
     */
    private void handleWsTimeseriesSubscriptionByKeys(TelemetryWebSocketSessionRef sessionRef,
                                                      TimeseriesSubscriptionCmd cmd, String sessionId, EntityId entityId) {


        long startTs = 0;
        if (cmd.getTimeWindow() > 0) {

            List<String> keys = new ArrayList<>(getKeys(cmd).orElse(Collections.emptySet()));
            log.debug("[{}] fetching timeseries data for last {} ms for keys: ({}) for device : {}", sessionId, cmd.getTimeWindow(), cmd.getKeys(), entityId);

            /***
             * 实时一天
             */

            long endTs = cmd.getStartTs() + cmd.getTimeWindow();

            if (cmd.getTimeWindow() >= 90000000l*2 && keys.size() >1) {
                startTs = cmd.getStartTs();
                final FutureCallback<List<TsKvEntry>> callback = getSubscriptionCallback(sessionRef, cmd, sessionId, entityId, startTs, keys);
                List<ReadTsKvQuery> que = keys.stream().map(key -> new BaseReadTsKvQuery(key, cmd.getStartTs(), cmd.getStartTs()+ DateUtils.getHourTime() / 30, cmd.getInterval(),
                        getLimit(cmd.getLimit()), getAggregation(cmd.getAgg()))).collect(Collectors.toList());
                accessValidator.validate(sessionRef.getSecurityCtx(), Operation.READ_TELEMETRY, entityId,
                        on(r -> Futures.addCallback(tsService.findAll(sessionRef.getSecurityCtx().getTenantId(), entityId, que), callback, executor), callback::onFailure));
            } else {
                startTs = cmd.getStartTs();
                final FutureCallback<List<TsKvEntry>> callback = getSubscriptionCallback(sessionRef, cmd, sessionId, entityId, startTs, keys);


                List<ReadTsKvQuery> queries = keys.stream().map(key -> new BaseReadTsKvQuery(key, cmd.getStartTs(), endTs, cmd.getInterval(),
                        getLimit(cmd.getLimit()), getAggregation(cmd.getAgg()))).collect(Collectors.toList());
                accessValidator.validate(sessionRef.getSecurityCtx(), Operation.READ_TELEMETRY, entityId,
                        on(r -> Futures.addCallback(tsService.findAll(sessionRef.getSecurityCtx().getTenantId(), entityId, queries), callback, executor), callback::onFailure));
            }
        } else {
            List<String> keys = new ArrayList<>(getKeys(cmd).orElse(Collections.emptySet()));
            startTs = System.currentTimeMillis();
            log.debug("[{}] fetching latest timeseries data for keys: ({}) for device : {}", sessionId, cmd.getKeys(), entityId);
            final FutureCallback<List<TsKvEntry>> callback = getSubscriptionCallback(sessionRef, cmd, sessionId, entityId, startTs, keys);
            accessValidator.validate(sessionRef.getSecurityCtx(), Operation.READ_TELEMETRY, entityId,
                    on(r -> Futures.addCallback(tsService.findLatest(sessionRef.getSecurityCtx().getTenantId(), entityId, keys), callback, executor), callback::onFailure));
        }
    }


    private void handleWsTimeseriesSubscription(TelemetryWebSocketSessionRef sessionRef,
                                                TimeseriesSubscriptionCmd cmd, String sessionId, EntityId entityId) {
        FutureCallback<List<TsKvEntry>> callback = new FutureCallback<List<TsKvEntry>>() {
            @Override
            public void onSuccess(List<TsKvEntry> data) {
                sendWsMsg(sessionRef, new SubscriptionUpdate(cmd.getCmdId(), data));
                Map<String, Long> subState = new HashMap<>(data.size());
                data.forEach(v -> subState.put(v.getKey(), v.getTs()));
                SubscriptionState sub = new SubscriptionState(sessionId, cmd.getCmdId(), sessionRef.getSecurityCtx().getTenantId(), entityId, TelemetryFeature.TIMESERIES, true, subState, cmd.getScope());
                subscriptionManager.addLocalWsSubscription(sessionId, entityId, sub);
            }

            @Override
            public void onFailure(Throwable e) {
                SubscriptionUpdate update;
                if (UnauthorizedException.class.isInstance(e)) {
                    update = new SubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.UNAUTHORIZED,
                            SubscriptionErrorCode.UNAUTHORIZED.getDefaultMsg());
                } else {
                    update = new SubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR,
                            FAILED_TO_FETCH_DATA);
                }
                sendWsMsg(sessionRef, update);
            }
        };
        accessValidator.validate(sessionRef.getSecurityCtx(), Operation.READ_TELEMETRY, entityId,
                on(r -> Futures.addCallback(tsService.findAllLatest(sessionRef.getSecurityCtx().getTenantId(), entityId), callback, executor), callback::onFailure));
    }


    private FutureCallback<List<TsKvEntry>> getSubscriptionCallback(final TelemetryWebSocketSessionRef sessionRef, final TimeseriesSubscriptionCmd cmd, final String sessionId, final EntityId entityId, final long startTs, final List<String> keys) {

        return new FutureCallback<List<TsKvEntry>>() {
            @Override
            public void onSuccess(List<TsKvEntry> data) {
                if (cmd.getTimeWindow() > 90000000l*2 && keys.size() >1) {
                    try {
                        data = newTableList();
                        if (keys.size() == 1) {
                            System.out.println(keys+"请求的参数");
                            data = data.stream().filter(s -> s.getKey().equals(keys.get(0))).collect(Collectors.toList());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }


                sendWsMsg(sessionRef, new SubscriptionUpdate(cmd.getCmdId(), data));
                Map<String, Long> subState = new HashMap<>(keys.size());


                keys.forEach(key -> subState.put(key, startTs));
                data.forEach(v -> subState.put(v.getKey(), v.getTs()));


                SubscriptionState sub = new SubscriptionState(sessionId, cmd.getCmdId(), sessionRef.getSecurityCtx().getTenantId(), entityId, TelemetryFeature.TIMESERIES, false, subState, cmd.getScope());
                subscriptionManager.addLocalWsSubscription(sessionId, entityId, sub);
            }

            @Override
            public void onFailure(Throwable e) {
                if (e instanceof TenantRateLimitException || e.getCause() instanceof TenantRateLimitException) {
                    log.trace("[{}] Tenant rate limit detected for subscription: [{}]:{}", sessionRef.getSecurityCtx().getTenantId(), entityId, cmd);
                } else {
                    log.info(FAILED_TO_FETCH_DATA, e);
                }
                SubscriptionUpdate update = new SubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR,
                        FAILED_TO_FETCH_DATA);
                sendWsMsg(sessionRef, update);
            }


            // 获取到小时为单位的数据
            private List<TsKvEntry> newTableList() throws Exception {
                List<TsKvEntry> entryList = new LinkedList<>();
                List<TsKvHourHistoryEntity> list = this.findAll();
                for (int i = 0; i < list.size(); i++) {
                    int finalI = i;
                    TsKvEntry tsKvEntry = new TsKvEntry() {
                        // 鼠标的显示
                        @Override
                        public long getTs() {
                            return (list.get(finalI).getTs());
                        }

                        // 类型的显示
                        @Override
                        public String getKey() {
                            return (list.get(finalI).getKey());
                        }

                        @Override
                        public DataType getDataType() {
                            return DataType.STRING;
                        }

                        @Override
                        public Optional<String> getStrValue() {
                            return Optional.ofNullable(String.valueOf(list.get(finalI).getTs()));
                        }

                        @Override
                        public Optional<Long> getLongValue() {
                            return Optional.ofNullable(list.get(finalI).getTs());
                        }

                        @Override
                        public Optional<Boolean> getBooleanValue() {
                            return Optional.ofNullable(true);
                        }

                        @Override
                        public Optional<Double> getDoubleValue() {
                            return Optional.empty();
                        }

                        @Override
                        public String getValueAsString() {
                            return String.valueOf(list.get(finalI).getValue());
                        }

                        @Override
                        public Object getValue() {
                            return cmd.getCmdId();
                        }
                    };
                    entryList.add(tsKvEntry);
                }
                return entryList;
            }


            private List<TsKvHourHistoryEntity> findAll() throws Exception {
                List<TsKvHourHistoryEntity> list = new LinkedList<>();


                /***
                 * 7天的量
                 */
                if(cmd.getTimeWindow()>=600000000 && cmd.getTimeWindow()<2590000000l){
                    cmd.setStartTs(System.currentTimeMillis() - DateUtils.getDayTime()*7);

                    /***
                     * 30天的量l
                     */
                }else if(cmd.getTimeWindow()>=2590000000l){
                    cmd.setStartTs(System.currentTimeMillis() - DateUtils.getDayTime()*30 );

                }
                boolean flag = true;

                while (flag) {
                    try {
                        Set<String> set = stringRedisTemplate.opsForZSet().rangeByScore(cmd.getEntityId(), cmd.getStartTs() - DateUtils.getDayTime(), cmd.getStartTs() + cmd.getTimeWindow());
                        for (String s : set) {
                            List<TsKvHourHistoryEntity> array = JSON.parseArray(s, TsKvHourHistoryEntity.class);
                            list.addAll(array);
                        }
                        flag = false;
                    } catch (Exception e) {
                        e.printStackTrace();
                        Thread.sleep(DateUtils.getHourTime() / 1200);
                        flag = true;
                    }
                }
                return list;
            }
        };
    }

    private void unsubscribe(TelemetryWebSocketSessionRef sessionRef, SubscriptionCmd cmd, String sessionId) {
        if (cmd.getEntityId() == null || cmd.getEntityId().isEmpty()) {
            subscriptionManager.cleanupLocalWsSessionSubscriptions(sessionRef, sessionId);
        } else {
            subscriptionManager.removeSubscription(sessionId, cmd.getCmdId());
        }
    }

    private boolean validateSubscriptionCmd(TelemetryWebSocketSessionRef sessionRef, SubscriptionCmd cmd) {
        if (cmd.getEntityId() == null || cmd.getEntityId().isEmpty()) {
            SubscriptionUpdate update = new SubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.BAD_REQUEST,
                    "Device id is empty!");
            sendWsMsg(sessionRef, update);
            return false;
        }
        return true;
    }

    private boolean validateSessionMetadata(TelemetryWebSocketSessionRef sessionRef, SubscriptionCmd cmd, String sessionId) {
        WsSessionMetaData sessionMD = wsSessionsMap.get(sessionId);
        if (sessionMD == null) {
            log.warn("[{}] Session meta data not found. ", sessionId);
            SubscriptionUpdate update = new SubscriptionUpdate(cmd.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR,
                    SESSION_META_DATA_NOT_FOUND);
            sendWsMsg(sessionRef, update);
            return false;
        } else {
            return true;
        }
    }

    private void sendWsMsg(TelemetryWebSocketSessionRef sessionRef, SubscriptionUpdate update) {

        if (update.getData().get("AQI") != null) {
            update.setType("AQI");
        }


        if (update.getData().get("CH2O") != null && update.getData().get("tvoc") != null) {
            update.setType("qianTang");
        }




        executor.submit(() -> {
            try {
                msgEndpoint.send(sessionRef, update.getSubscriptionId(), jsonMapper.writeValueAsString(update));
            } catch (JsonProcessingException e) {
                log.warn("[{}] Failed to encode reply: {}", sessionRef.getSessionId(), update, e);
            } catch (IOException e) {
                log.warn("[{}] Failed to send reply: {}", sessionRef.getSessionId(), update, e);
            }
        });
    }

    private static Optional<Set<String>> getKeys(TelemetryPluginCmd cmd) {
        if (!StringUtils.isEmpty(cmd.getKeys())) {
            Set<String> keys = new HashSet<>();
            Collections.addAll(keys, cmd.getKeys().split(","));
            return Optional.of(keys);
        } else {
            return Optional.empty();
        }
    }

    private ListenableFuture<List<AttributeKvEntry>> mergeAllAttributesFutures(List<ListenableFuture<List<AttributeKvEntry>>> futures) {
        return Futures.transform(Futures.successfulAsList(futures),
                (Function<? super List<List<AttributeKvEntry>>, ? extends List<AttributeKvEntry>>) input -> {
                    List<AttributeKvEntry> tmp = new ArrayList<>();
                    if (input != null) {
                        input.forEach(tmp::addAll);
                    }
                    return tmp;
                }, executor);
    }

    private <T> FutureCallback<ValidationResult> getAttributesFetchCallback(final TenantId tenantId, final EntityId entityId, final List<String> keys, final FutureCallback<List<AttributeKvEntry>> callback) {
        return new FutureCallback<ValidationResult>() {
            @Override
            public void onSuccess(@Nullable ValidationResult result) {
                List<ListenableFuture<List<AttributeKvEntry>>> futures = new ArrayList<>();
                for (String scope : DataConstants.allScopes()) {
                    futures.add(attributesService.find(tenantId, entityId, scope, keys));
                }

                ListenableFuture<List<AttributeKvEntry>> future = mergeAllAttributesFutures(futures);
                Futures.addCallback(future, callback);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        };
    }

    private <T> FutureCallback<ValidationResult> getAttributesFetchCallback(final TenantId tenantId, final EntityId entityId, final String scope, final List<String> keys, final FutureCallback<List<AttributeKvEntry>> callback) {
        return new FutureCallback<ValidationResult>() {
            @Override
            public void onSuccess(@Nullable ValidationResult result) {
                Futures.addCallback(attributesService.find(tenantId, entityId, scope, keys), callback);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        };
    }

    private <T> FutureCallback<ValidationResult> getAttributesFetchCallback(final TenantId tenantId, final EntityId entityId, final FutureCallback<List<AttributeKvEntry>> callback) {
        return new FutureCallback<ValidationResult>() {
            @Override
            public void onSuccess(@Nullable ValidationResult result) {
                List<ListenableFuture<List<AttributeKvEntry>>> futures = new ArrayList<>();
                for (String scope : DataConstants.allScopes()) {
                    futures.add(attributesService.findAll(tenantId, entityId, scope));
                }

                ListenableFuture<List<AttributeKvEntry>> future = mergeAllAttributesFutures(futures);
                Futures.addCallback(future, callback);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        };
    }

    private <T> FutureCallback<ValidationResult> getAttributesFetchCallback(final TenantId tenantId, final EntityId entityId, final String scope, final FutureCallback<List<AttributeKvEntry>> callback) {
        return new FutureCallback<ValidationResult>() {
            @Override
            public void onSuccess(@Nullable ValidationResult result) {
                Futures.addCallback(attributesService.findAll(tenantId, entityId, scope), callback);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        };
    }

    private FutureCallback<ValidationResult> on(Consumer<Void> success, Consumer<Throwable> failure) {
        return new FutureCallback<ValidationResult>() {
            @Override
            public void onSuccess(@Nullable ValidationResult result) {
                ValidationResultCode resultCode = result.getResultCode();
                if (resultCode == ValidationResultCode.OK) {
                    success.accept(null);
                } else {
                    onFailure(ValidationCallback.getException(result));
                }
            }

            @Override
            public void onFailure(Throwable t) {
                failure.accept(t);
            }
        };
    }


    private static Aggregation getAggregation(String agg) {
        return StringUtils.isEmpty(agg) ? DEFAULT_AGGREGATION : Aggregation.valueOf(agg);
    }

    private int getLimit(int limit) {
        return limit == 0 ? DEFAULT_LIMIT : limit;
    }
}
