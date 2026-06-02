package com.ssafy.demo_app.domain.routing.repository;

import com.ssafy.demo_app.domain.routing.entity.FactoryRouting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FactoryRoutingRepository extends JpaRepository<FactoryRouting, Integer> {

    boolean existsByFactoryNameAndLineNameAndOperationSeq(
            String factoryName,
            String lineName,
            Integer operationSeq
    );

    boolean existsByFactoryNameAndLineNameAndOperationSeqAndRoutingIdNot(
            String factoryName,
            String lineName,
            Integer operationSeq,
            Integer routingId
    );

    List<FactoryRouting> findByFactoryNameOrderByLineNameAscOperationSeqAsc(String factoryName);

    List<FactoryRouting> findByFactoryNameAndLineNameOrderByOperationSeqAsc(
            String factoryName,
            String lineName
    );

    List<FactoryRouting> findAllByOrderByFactoryNameAscLineNameAscOperationSeqAsc();

    @Query("select distinct r.factoryName from FactoryRouting r order by r.factoryName asc")
    List<String> findDistinctFactoryNames();

    @Query("select distinct r.lineName from FactoryRouting r where r.factoryName = :factoryName order by r.lineName asc")
    List<String> findDistinctLineNamesByFactoryName(String factoryName);
}
