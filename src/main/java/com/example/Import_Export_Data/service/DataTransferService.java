package com.example.Import_Export_Data.service;

import com.example.Import_Export_Data.entity.destination.*;
import com.example.Import_Export_Data.entity.source.*;
import com.example.Import_Export_Data.repository.destination.*;
import com.example.Import_Export_Data.repository.source.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DataTransferService {

    @Autowired
    private SourceMasterChartOfAccountRepository sourceMasterChartOfAccountRepository;
    @Autowired
    private DestinationMasterChartOfAccountRepository destinationMasterChartOfAccountRepository;
    @Autowired
    private SourceAccountProductionMasterSectionRepository sourceSectionsRepository;
    @Autowired
    private DestinationAccountProductionMasterSectionRepository destinationSectionsRepository;
    @Autowired
    private SourceAccountProductionSubSectionsRepository sourceSubSectionRepository;
    @Autowired
    private DestinationAccountProductionSubSectionsRepository destinationSubSectionRepository;
    @Autowired
    private SourceAccountProductionMasterTableRepository sourceMasterTableRepository;
    @Autowired
    private DestinationAccountProductionMasterTableRepository destinationMasterTableRepository;

    @Transactional
    public void transferById(int sourceId) {
        MasterChartOfAccount sourceAccount = sourceMasterChartOfAccountRepository.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Source account with ID " + sourceId + " not found."));

        // Soft Delete Existing Destination Records (if any)
        List<DestinationMasterChartOfAccount> existingAccounts =
                destinationMasterChartOfAccountRepository.findByChartOfAccountName(sourceAccount.getChartofaccountname());

        if (!existingAccounts.isEmpty()) {
            for (DestinationMasterChartOfAccount oldAccount : existingAccounts) {
                // Soft delete Master Chart of Account
                destinationMasterChartOfAccountRepository.softDeleteByMasterChartOfAccountId(oldAccount.getId());

                // Soft delete related records
                destinationSectionsRepository.softDeleteByApVersion(oldAccount.getId());
                destinationSubSectionRepository.softDeleteByMasterChartOfAccountId(oldAccount.getId());
                destinationMasterTableRepository.softDeleteByMasterChartOfAccountId(oldAccount.getId());
            }
        }

        // Insert new Master Chart of Account
        DestinationMasterChartOfAccount newDestinationAccount = new DestinationMasterChartOfAccount();
        newDestinationAccount.setChartOfAccountName(sourceAccount.getChartofaccountname());
        newDestinationAccount.setDescription(sourceAccount.getDescription());
        newDestinationAccount.setP_version(sourceAccount.getP_version());
        newDestinationAccount.setPrev_chart_of_account_id(sourceAccount.getPrev_chart_of_account_id());
        newDestinationAccount.setIndustry_id(sourceAccount.getIndustry_id());
        newDestinationAccount.setDeleted(sourceAccount.getIs_deleted());
        newDestinationAccount.setIs_default(sourceAccount.getIs_default());
        newDestinationAccount.setCreated_by(sourceAccount.getCreated_by());
        newDestinationAccount.setCreated_date(sourceAccount.getCreated_date());
        newDestinationAccount.setUpdated_by(sourceAccount.getUpdated_by());
        newDestinationAccount.setUpdated_date(sourceAccount.getUpdated_date());
        newDestinationAccount.setIs_active(sourceAccount.getIs_active());
        newDestinationAccount.setStatus_id(sourceAccount.getStatus_id());
        newDestinationAccount.setIs_versioning(sourceAccount.getIs_versioning());
        newDestinationAccount.setIs_copy_chartofaccount(sourceAccount.getIs_copy_chartofaccount());
        newDestinationAccount.setCountryId(sourceAccount.getCountryId());
        newDestinationAccount.setSubstantialChange(sourceAccount.getSubstantialChange());

        newDestinationAccount = destinationMasterChartOfAccountRepository.saveAndFlush(newDestinationAccount);
        final int destinationAccountId = newDestinationAccount.getId(); // Extract ID before lambda

        // Soft Delete Existing Sections in Destination
        List<DestinationAccountProductionMasterSection> existingSections = destinationSectionsRepository.findAllByApVersion(destinationAccountId);
        for (DestinationAccountProductionMasterSection oldSection : existingSections) {
            if (!oldSection.getDeleted()) { // Only update if it's currently false
                oldSection.setDeleted(true);
            }
        }
        destinationSectionsRepository.saveAll(existingSections);

// Transfer Sections with Correct is_deleted Handling
        List<AccountProductionMasterSection> sourceSections = sourceSectionsRepository.findAllByApVersion(sourceId);
        List<DestinationAccountProductionMasterSection> destinationSections = sourceSections.stream().map(section -> {
            DestinationAccountProductionMasterSection destinationSection = new DestinationAccountProductionMasterSection();
            destinationSection.setApVersion(destinationAccountId);
            destinationSection.setMenuName(section.getMenuName());

            // Maintain the same is_deleted status as source
            destinationSection.setDeleted(section.getDeleted());

            // Copy all other fields
            destinationSection.setParentId(section.getParentId());
            destinationSection.setSerialNumber(section.getSerialNumber());
            destinationSection.setMenuType(section.getMenuType());
            destinationSection.setEditable(section.getEditable());
            destinationSection.setActive(section.getActive());
            destinationSection.setCreatedBy(section.getCreatedBy());
            destinationSection.setCreatedDate(section.getCreatedDate());
            destinationSection.setUpdatedBy(section.getUpdatedBy());
            destinationSection.setUpdatedDate(section.getUpdatedDate());
            destinationSection.setAliasName(section.getAliasName());
            destinationSection.setOriginalSectionId(section.getOriginalSectionId());

            return destinationSection;
        }).collect(Collectors.toList());

        destinationSectionsRepository.saveAll(destinationSections);


        // Soft delete existing sub-sections before inserting new ones
        List<DestinationAccountProductionSubSections> existingSubSections =
                destinationSubSectionRepository.findByMasterChartOfAccountId(destinationAccountId);

        for (DestinationAccountProductionSubSections oldSubSection : existingSubSections) {
            if (!oldSubSection.getDeleted()) { // Only update if it's currently false
                oldSubSection.setDeleted(true);
            }
        }
        destinationSubSectionRepository.saveAll(existingSubSections);

// Transfer new sub-sections
        List<AccountProductionSubSections> sourceSubSections =
                sourceSubSectionRepository.findByMasterChartOfAccountId(sourceId);

        List<DestinationAccountProductionSubSections> destinationSubSections = sourceSubSections.stream().map(subSection -> {
            DestinationAccountProductionSubSections destinationSubSection = new DestinationAccountProductionSubSections();
            destinationSubSection.setMasterChartOfAccountId(destinationAccountId);
            destinationSubSection.setMasterSectionId(subSection.getMasterSectionId());
            destinationSubSection.setMenuName(subSection.getMenuName());
            destinationSubSection.setSerialNumber(subSection.getSerialNumber());
            destinationSubSection.setMenuType(subSection.getMenuType());
            destinationSubSection.setEditable(subSection.getEditable());
            destinationSubSection.setActive(subSection.getActive());
            destinationSubSection.setDeleted(subSection.getDeleted()); // Preserve original isDeleted value
            destinationSubSection.setCreatedBy(subSection.getCreatedBy());
            destinationSubSection.setCreatedDate(subSection.getCreatedDate());
            destinationSubSection.setUpdatedBy(subSection.getUpdatedBy());
            destinationSubSection.setUpdatedDate(subSection.getUpdatedDate());
            destinationSubSection.setApVersion(subSection.getApVersion());
            destinationSubSection.setAliasName(subSection.getAliasName());
            destinationSubSection.setLoader(subSection.getLoader());
            destinationSubSection.setSectionSequence(subSection.getSectionSequence());
            destinationSubSection.setCashFlowType(subSection.getCashFlowType());
            destinationSubSection.setOriginalSubSectionId(subSection.getOriginalSubSectionId());

            return destinationSubSection;
        }).collect(Collectors.toList());

        destinationSubSectionRepository.saveAll(destinationSubSections);


        // Transfer Master Tables
        // Soft delete existing master tables before inserting new ones
        List<DestinationAccountProductionMasterTable> existingMasterTables =
                destinationMasterTableRepository.findAllByMasterChartOfAccountId(destinationAccountId);

        for (DestinationAccountProductionMasterTable oldTable : existingMasterTables) {
            if (!oldTable.getDeleted()) { // Only update if it's currently false
                oldTable.setDeleted(true);
            }
        }
        destinationMasterTableRepository.saveAll(existingMasterTables);

// Transfer new master tables from source to destination
        List<AccountProductionMasterTable> sourceMasterTables =
                sourceMasterTableRepository.findAllByMasterChartOfAccountId(sourceId);

        List<DestinationAccountProductionMasterTable> destinationMasterTables = sourceMasterTables.stream().map(table -> {
            DestinationAccountProductionMasterTable destinationTable = new DestinationAccountProductionMasterTable();
            destinationTable.setMasterChartOfAccountId(destinationAccountId);
            destinationTable.setMasterSectionId(table.getMasterSectionId());
            destinationTable.setSubSectionId(table.getSubSectionId());
            destinationTable.setMenuName(table.getMenuName());
            destinationTable.setTableJson(table.getTableJson());
            destinationTable.setReferenceId(table.getReferenceId());
            destinationTable.setEditable(table.getEditable());
            destinationTable.setActive(table.getActive());
            destinationTable.setDeleted(table.getDeleted()); // Preserve original isDeleted value
            destinationTable.setCreatedBy(table.getCreatedBy());
            destinationTable.setCreatedDate(table.getCreatedDate());
            destinationTable.setUpdatedBy(table.getUpdatedBy());
            destinationTable.setUpdatedDate(table.getUpdatedDate());
            destinationTable.setApVersion(table.getApVersion());
            destinationTable.setHeaderType(table.getHeaderType());
            destinationTable.setChecklistQuestionReference(table.getChecklistQuestionReference());
            destinationTable.setSequenceNumber(table.getSequenceNumber());
            destinationTable.setFsaAreaId(table.getFsaAreaId());
            destinationTable.setOriginalTableId(table.getOriginalTableId());
            destinationTable.setCkeditorCheck(table.getCkeditorCheck());

            return destinationTable;
        }).collect(Collectors.toList());

        destinationMasterTableRepository.saveAll(destinationMasterTables);

    }
}
