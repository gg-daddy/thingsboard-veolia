package org.thingsboard.server.service.feedback;/*
 * @Author:${zhangrui}
 * @Date:2020/9/23 17:47
 */

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.thingsboard.server.Request.FeedbackRequest;
import org.thingsboard.server.Response.QueryPaginationResult;
import org.thingsboard.server.dao.model.sql.FeedbackEntity;
import org.thingsboard.server.dao.model.sql.ProjectEntity;
import org.thingsboard.server.dao.sql.Feedback.FeedbackRepository;
import org.thingsboard.server.dao.sql.projectList.ProjectRepository;
import org.thingsboard.server.utils.DateUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DefaultFeedbackServer implements FeedbackServer {

    @Autowired
    private FeedbackRepository feedbackRepository;



    /***
     * 添加
     * @param feedbackRequest
     */
    @Override
    public void add(FeedbackRequest feedbackRequest) {

        FeedbackEntity feedbackEntity = new FeedbackEntity();
        feedbackEntity.setId(UUID.randomUUID().toString());
        feedbackEntity.setContact(feedbackRequest.getContact());
        feedbackEntity.setContent(feedbackRequest.getContent());
        feedbackEntity.setProjectId(feedbackRequest.getProjectId());
        feedbackEntity.setDate(DateUtils.getNow());
        feedbackEntity.setName(feedbackRequest.getName());

        feedbackRepository.save(feedbackEntity);


    }

    /***
     * 查询
     * @param pageNo
     * @param pageSize
     * @return
     */
    @Override
    public QueryPaginationResult findAll(String pageNo, String pageSize) {
        Sort sort = Sort.by(Sort.Direction.DESC, "date");
        Pageable pageable = PageRequest.of(Integer.valueOf(pageNo), Integer.valueOf(pageSize), sort);
        Specification<FeedbackEntity> query = new Specification<FeedbackEntity>() {
            @Override
            public Predicate toPredicate(Root<FeedbackEntity> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicates = new ArrayList<>();
                return criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()]));
            }

        };
        Page<FeedbackEntity> entityPage = feedbackRepository.findAll(query, pageable);
        QueryPaginationResult queryPaginationResult = new QueryPaginationResult();
        queryPaginationResult.setTotal(entityPage.getTotalElements());
        queryPaginationResult.setPageSize(Integer.valueOf(pageSize));
        queryPaginationResult.setTotalPage(entityPage.getTotalPages());
        queryPaginationResult.setPage(this.change(entityPage.getContent()));


        return queryPaginationResult;
    }


    @Autowired
    ProjectRepository projectRepository;
    /***
     * 数据对换
     * @return
     */
    private List<FeedbackEntity> change(List<FeedbackEntity> feedbackEntityList) {

        List<String> idList = feedbackEntityList.stream().map(FeedbackEntity::getProjectId).collect(Collectors.toList());

         //项目列表
        List<ProjectEntity> projectListEntityList = projectRepository.findByIdIn(idList);

        /***
         * 双循环过滤
         */
        for (int i = 0; i < feedbackEntityList.size(); i++) {
            for (int j = 0; j <projectListEntityList.size() ; j++) {


                if(feedbackEntityList.get(i).getProjectId().equals(projectListEntityList.get(j).getId())){

                    feedbackEntityList.get(i).setProjectId(projectListEntityList.get(j).getName());
                    break;
                }

            }
        }

        return feedbackEntityList;


    }


}
