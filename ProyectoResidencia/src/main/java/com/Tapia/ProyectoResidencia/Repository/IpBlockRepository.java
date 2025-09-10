package com.Tapia.ProyectoResidencia.Repository;

import com.Tapia.ProyectoResidencia.Model.IpBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IpBlockRepository extends JpaRepository<IpBlock, Long> {
    Optional<IpBlock> findByIp(String ip);
}
