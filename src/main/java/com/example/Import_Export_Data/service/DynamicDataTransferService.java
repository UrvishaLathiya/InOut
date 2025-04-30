package com.example.Import_Export_Data.service;

import com.example.Import_Export_Data.DTO.DestinationDbConfig;
import com.example.Import_Export_Data.config.DynamicDatabaseConfig;
import com.example.Import_Export_Data.entity.destination.*;
import com.example.Import_Export_Data.entity.source.*;
import com.example.Import_Export_Data.repository.source.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;

@Service
public class DynamicDataTransferService {

    @Autowired
    private SourceMasterChartOfAccountRepository sourceChartRepo;
    @Autowired
    private SourceAccountProductionMasterSectionRepository sourceSectionsRepo;
    @Autowired
    private SourceAccountProductionSubSectionsRepository sourceSubSectionsRepo;
    @Autowired
    private SourceAccountProductionMasterTableRepository sourceTablesRepo;

    @Autowired
    private DynamicDatabaseConfig dynamicDatabaseConfig;

    @Transactional
    public void transferToDynamicDestination(Integer sourceId, DestinationDbConfig config) throws Exception {
        DataSource dynamicDataSource = dynamicDatabaseConfig.createDataSource(config);
        EntityManagerFactory emf = dynamicDatabaseConfig.createEntityManagerFactory(dynamicDataSource);
        EntityManager em = emf.createEntityManager();

        em.getTransaction().begin();

        try {
            em.createNativeQuery("SELECT 1").getSingleResult();

            // 1. Fetch source MasterChart
            MasterChartOfAccount source = sourceChartRepo.findById(sourceId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid sourceId: " + sourceId));

            // 2. Soft delete existing destination data by chart name
            List<DestinationMasterChartOfAccount> existingAccounts = em
                    .createQuery("SELECT d FROM DestinationMasterChartOfAccount d WHERE d.chartOfAccountName = :name", DestinationMasterChartOfAccount.class)
                    .setParameter("name", source.getChartofaccountname())
                    .getResultList();

            for (DestinationMasterChartOfAccount oldAccount : existingAccounts) {
                oldAccount.setDeleted(true);
                em.merge(oldAccount);

                em.createQuery("UPDATE DestinationAccountProductionMasterSection s SET s.isDeleted = true WHERE s.apVersion = :id")
                        .setParameter("id", oldAccount.getId())
                        .executeUpdate();

                em.createQuery("UPDATE DestinationAccountProductionSubSections s SET s.isDeleted = true WHERE s.masterChartOfAccountId = :id")
                        .setParameter("id", oldAccount.getId())
                        .executeUpdate();

                em.createQuery("UPDATE DestinationAccountProductionMasterTable t SET t.isDeleted = true WHERE t.masterChartOfAccountId = :id")
                        .setParameter("id", oldAccount.getId())
                        .executeUpdate();
            }

            // 3. Insert MasterChart
            DestinationMasterChartOfAccount destChart = new DestinationMasterChartOfAccount(source);
            em.persist(destChart);
            em.flush();
            int destId = destChart.getId();

            // 4. Transfer Sections
            List<AccountProductionMasterSection> sourceSections = sourceSectionsRepo.findAllByApVersion(sourceId);
            for (AccountProductionMasterSection s : sourceSections) {
                DestinationAccountProductionMasterSection d = new DestinationAccountProductionMasterSection(s);
                d.setApVersion(destId);
                em.persist(d);
            }

            // 5. Transfer SubSections
            List<AccountProductionSubSections> sourceSubSections = sourceSubSectionsRepo.findByMasterChartOfAccountId(sourceId);
            for (AccountProductionSubSections s : sourceSubSections) {
                DestinationAccountProductionSubSections d = new DestinationAccountProductionSubSections(s);
                d.setMasterChartOfAccountId(destId);
                em.persist(d);
            }

            // 6. Transfer Tables
            List<AccountProductionMasterTable> sourceTables = sourceTablesRepo.findAllByMasterChartOfAccountId(sourceId);
            for (AccountProductionMasterTable t : sourceTables) {
                DestinationAccountProductionMasterTable d = new DestinationAccountProductionMasterTable(t);
                d.setMasterChartOfAccountId(destId);
                em.persist(d);
            }

            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
            throw new RuntimeException("Transfer failed: " + e.getMessage());
        } finally {
            em.close();
            emf.close();
        }


    }
}
