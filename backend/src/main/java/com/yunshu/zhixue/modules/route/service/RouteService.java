package com.yunshu.zhixue.modules.route.service;

import com.yunshu.zhixue.modules.route.dto.AddRouteCoursesRequest;
import com.yunshu.zhixue.modules.route.dto.CreateRouteRequest;
import com.yunshu.zhixue.modules.route.dto.RouteCardVO;
import com.yunshu.zhixue.modules.route.dto.RouteDetailVO;
import com.yunshu.zhixue.modules.route.dto.SortRouteCoursesRequest;
import com.yunshu.zhixue.modules.route.dto.UpdateRouteRequest;
import com.yunshu.zhixue.modules.route.dto.UpdateRouteStatusRequest;
import com.yunshu.zhixue.modules.system.dto.Actor;

import java.util.List;

/**
 * 管理端学习路线管理（A-R01 ~ A-R09）。
 */
public interface RouteService {

    List<RouteCardVO> list();

    void create(CreateRouteRequest request, Actor actor);

    void update(Long id, UpdateRouteRequest request, Actor actor);

    void updateStatus(Long id, UpdateRouteStatusRequest request, Actor actor);

    void delete(Long id, Actor actor);

    RouteDetailVO detail(Long id);

    void addCourses(Long id, AddRouteCoursesRequest request, Actor actor);

    void removeCourse(Long id, Long courseId, Actor actor);

    void sortCourses(Long id, SortRouteCoursesRequest request, Actor actor);
}
