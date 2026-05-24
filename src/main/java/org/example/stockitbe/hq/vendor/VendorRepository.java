package org.example.stockitbe.hq.vendor;

import org.example.stockitbe.hq.vendor.model.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VendorRepository extends JpaRepository<Vendor, Long> {

    Optional<Vendor> findByCode(String code);
}
