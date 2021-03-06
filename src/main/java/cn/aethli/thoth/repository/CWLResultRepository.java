package cn.aethli.thoth.repository;

import cn.aethli.thoth.entity.CWLResult;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** @author Termite */
public interface CWLResultRepository
    extends JpaRepository<CWLResult, String>, JpaSpecificationExecutor<CWLResult> {

  @Query("FROM CWLResult c WHERE c.name=:typeName")
  List<CWLResult> findByType(@Param("typeName") String typeName);
}
