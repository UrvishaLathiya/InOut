package com.example.Import_Export_Data.service;

import com.example.Import_Export_Data.DTO.FullLedgerInfoDTO;
import com.example.Import_Export_Data.entity.ledgerEntity.Ledger;
import com.example.Import_Export_Data.repository.ledgerRepo.LedgerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.sql.Timestamp;

@Service
public class LedgerService {

    private final LedgerRepository ledgerRepository;
    private final JdbcTemplate ledgerJdbcTemplate;

    public LedgerService(LedgerRepository ledgerRepository, 
                        @Qualifier("ledgerJdbcTemplate") JdbcTemplate ledgerJdbcTemplate) {
        this.ledgerRepository = ledgerRepository;
        this.ledgerJdbcTemplate = ledgerJdbcTemplate;
    }

    @Transactional
    public void resetSequence() {
        // Get the maximum ID from the table
        Integer maxId = ledgerJdbcTemplate.queryForObject(
            "SELECT COALESCE(MAX(id), 0) FROM master.ledger", Integer.class);
        
        // Reset the sequence to the next value after maxId
        ledgerJdbcTemplate.execute(
            "ALTER SEQUENCE master.ledger_id_seq RESTART WITH " + (maxId + 1));
    }

    public List<FullLedgerInfoDTO> getAllLedger() {
        return ledgerRepository.findAllLedgerWithFullGroupInfo(); // ✅ Use this directly
    }

    public List<String> getDistinctApVersions() {
        return ledgerRepository.findDistinctApVersions();
    }

    @Transactional
    public boolean updateLedgerField(Integer id, String columnName, String value) {
        System.out.println("Attempting to update ledger - ID: " + id + ", Column: " + columnName + ", Value: " + value);
        
        Optional<Ledger> optionalLedger = ledgerRepository.findById(id);
        if (optionalLedger.isEmpty()) {
            System.out.println("Ledger not found with ID: " + id);
            return false;
        }

        Ledger ledger = optionalLedger.get();
        System.out.println("Found ledger: " + ledger);
        
        // Map column names to their corresponding setter methods
        Map<String, Runnable> fieldSetters = new HashMap<>();
        
        // Add all possible field updates
        fieldSetters.put("ID", () -> ledger.setId(Integer.parseInt(value)));
        fieldSetters.put("Ledger Name", () -> ledger.setLedgerName(value));
        fieldSetters.put("Created Date", () -> ledger.setCreatedDate(java.sql.Timestamp.valueOf(value)));
        fieldSetters.put("Is Active", () -> ledger.setActive(Boolean.parseBoolean(value)));
        fieldSetters.put("Is Deleted", () -> ledger.setDeleted(Boolean.parseBoolean(value)));
        fieldSetters.put("Updated Date", () -> ledger.setUpdatedDate(java.sql.Timestamp.valueOf(value)));
        fieldSetters.put("Version", () -> ledger.setVersion(Integer.parseInt(value)));
        fieldSetters.put("Code", () -> ledger.setCode(value));
        fieldSetters.put("Is Group", () -> ledger.setGroup(Boolean.parseBoolean(value)));
        fieldSetters.put("Is Ledger", () -> ledger.setLedger(Boolean.parseBoolean(value)));
        fieldSetters.put("Is SubGroup", () -> ledger.setSubGroup(Boolean.parseBoolean(value)));
        fieldSetters.put("Created By", () -> ledger.setCreatedBy(Integer.parseInt(value)));
        fieldSetters.put("Updated By", () -> ledger.setUpdatedBy(Integer.parseInt(value)));
        fieldSetters.put("Ledger Type ID", () -> ledger.setLedgerTypeId(Short.parseShort(value)));
        fieldSetters.put("Parent ID", () -> ledger.setParentId(Integer.parseInt(value)));
        fieldSetters.put("Menu ID", () -> ledger.setTbMenuId(Integer.parseInt(value)));
        fieldSetters.put("Serial Number", () -> ledger.setSerialNumber(Integer.parseInt(value)));
        fieldSetters.put("Formula", () -> ledger.setFormula(value));
        fieldSetters.put("Is Editable", () -> ledger.setEditable(Boolean.parseBoolean(value)));
        fieldSetters.put("Depreciation Ledger ID", () -> ledger.setDepreciationLedgerId(Integer.parseInt(value)));
        fieldSetters.put("Accumulated Depreciation ID", () -> ledger.setAccumulatedDepreciationId(Integer.parseInt(value)));
        fieldSetters.put("Is Optional", () -> ledger.setOptional(Boolean.parseBoolean(value)));
        fieldSetters.put("AP Version", () -> ledger.setApVersion(Integer.parseInt(value)));
        fieldSetters.put("FSA Area ID", () -> ledger.setFsaAreaId(Short.parseShort(value)));
        fieldSetters.put("Ledger Header", () -> ledger.setLedgerHeader(value));

        // Execute the appropriate setter
        Runnable setter = fieldSetters.get(columnName);
        if (setter != null) {
            try {
                System.out.println("Updating field: " + columnName + " with value: " + value);
                // Update the field
                setter.run();
                
                // Always update the updated_date when any field is changed
                ledger.setUpdatedDate(new java.sql.Timestamp(System.currentTimeMillis()));
                
                // Save the changes
                System.out.println("Saving updated ledger: " + ledger);
                ledgerRepository.save(ledger);
                System.out.println("Update successful");
                return true;
            } catch (Exception e) {
                System.out.println("Error updating ledger: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
        
        System.out.println("No setter found for column: " + columnName);
        return false;
    }

    @Transactional
    public FullLedgerInfoDTO createLedger(Map<String, Object> request) {
        // Reset sequence before creating new ledger
        resetSequence();

        // Validate required fields
        if (!request.containsKey("ledgerName") || request.get("ledgerName") == null) {
            throw new IllegalArgumentException("Ledger Name is required");
        }
        if (!request.containsKey("version") || request.get("version") == null) {
            throw new IllegalArgumentException("Version is required");
        }
        if (!request.containsKey("apVersion") || request.get("apVersion") == null) {
            throw new IllegalArgumentException("AP Version is required");
        }
        if (!request.containsKey("createdBy") || request.get("createdBy") == null) {
            throw new IllegalArgumentException("Created By is required");
        }
        if (!request.containsKey("updatedBy") || request.get("updatedBy") == null) {
            throw new IllegalArgumentException("Updated By is required");
        }

        String ledgerName = (String) request.get("ledgerName");
        Integer version = Integer.parseInt(request.get("version").toString());
        Integer apVersion = Integer.parseInt(request.get("apVersion").toString());

        // Check for duplicate ledger
        Optional<Ledger> existingLedger = ledgerRepository.findByLedgerNameAndVersionAndApVersion(
            ledgerName, version, apVersion);
        if (existingLedger.isPresent()) {
            throw new IllegalArgumentException("A ledger with the same name, version, and AP version already exists");
        }

        Ledger ledger = new Ledger();
        
        // Set automatic fields
        ledger.setCreatedDate(new Timestamp(System.currentTimeMillis()));
        ledger.setUpdatedDate(new Timestamp(System.currentTimeMillis()));
        
        // Set default values for boolean fields
        ledger.setActive(request.containsKey("isActive") ? (Boolean) request.get("isActive") : false);
        ledger.setDeleted(request.containsKey("isDeleted") ? (Boolean) request.get("isDeleted") : false);
        ledger.setGroup(request.containsKey("isGroup") ? (Boolean) request.get("isGroup") : false);
        ledger.setLedger(request.containsKey("isLedger") ? (Boolean) request.get("isLedger") : true);
        ledger.setSubGroup(request.containsKey("isSubGroup") ? (Boolean) request.get("isSubGroup") : false);
        ledger.setEditable(request.containsKey("isEditable") ? (Boolean) request.get("isEditable") : true);
        ledger.setOptional(request.containsKey("isOptional") ? (Boolean) request.get("isOptional") : false);

        // Set user-provided fields with null checks
        ledger.setLedgerName(ledgerName);
        ledger.setVersion(version);
        ledger.setApVersion(apVersion);
        ledger.setCreatedBy(Integer.parseInt(request.get("createdBy").toString()));
        ledger.setUpdatedBy(Integer.parseInt(request.get("updatedBy").toString()));

        if (request.containsKey("code") && request.get("code") != null) {
            ledger.setCode((String) request.get("code"));
        }
        if (request.containsKey("ledgerTypeId") && request.get("ledgerTypeId") != null) {
            ledger.setLedgerTypeId(Short.parseShort(request.get("ledgerTypeId").toString()));
        }
        if (request.containsKey("parentId") && request.get("parentId") != null) {
            ledger.setParentId(Integer.parseInt(request.get("parentId").toString()));
        }
        if (request.containsKey("tbMenuId") && request.get("tbMenuId") != null) {
            ledger.setTbMenuId(Integer.parseInt(request.get("tbMenuId").toString()));
        }
        if (request.containsKey("serialNumber") && request.get("serialNumber") != null) {
            ledger.setSerialNumber(Integer.parseInt(request.get("serialNumber").toString()));
        }
        if (request.containsKey("formula") && request.get("formula") != null) {
            ledger.setFormula((String) request.get("formula"));
        }
        if (request.containsKey("depreciationLedgerId") && request.get("depreciationLedgerId") != null) {
            ledger.setDepreciationLedgerId(Integer.parseInt(request.get("depreciationLedgerId").toString()));
        }
        if (request.containsKey("accumulatedDepreciationId") && request.get("accumulatedDepreciationId") != null) {
            ledger.setAccumulatedDepreciationId(Integer.parseInt(request.get("accumulatedDepreciationId").toString()));
        }
        if (request.containsKey("fsaAreaId") && request.get("fsaAreaId") != null) {
            ledger.setFsaAreaId(Short.parseShort(request.get("fsaAreaId").toString()));
        }
        if (request.containsKey("ledgerHeader") && request.get("ledgerHeader") != null) {
            ledger.setLedgerHeader((String) request.get("ledgerHeader"));
        }

        // Save the ledger to get the auto-generated ID
        Ledger savedLedger = ledgerRepository.save(ledger);

        // Create FullLedgerInfoDTO
        FullLedgerInfoDTO dto = new FullLedgerInfoDTO(
            savedLedger.getId(),
            savedLedger.getCreatedDate(),
            savedLedger.getActive(),
            savedLedger.getDeleted(),
            savedLedger.getUpdatedDate(),
            savedLedger.getVersion(),
            savedLedger.getCode(),
            savedLedger.getGroup(),
            savedLedger.getLedger(),
            savedLedger.getSubGroup(),
            savedLedger.getLedgerName(),
            savedLedger.getCreatedBy(),
            savedLedger.getUpdatedBy(),
            savedLedger.getLedgerTypeId(),
            savedLedger.getParentId(),
            savedLedger.getTbMenuId(),
            savedLedger.getSerialNumber(),
            savedLedger.getFormula(),
            savedLedger.getEditable(),
            savedLedger.getDepreciationLedgerId(),
            savedLedger.getAccumulatedDepreciationId(),
            savedLedger.getOptional(),
            savedLedger.getApVersion(),
            savedLedger.getFsaAreaId(),
            savedLedger.getLedgerHeader(),
            null, // groupId
            null, // groupName
            null, // subGroupId
            null  // subGroupName
        );

        // If parentId is provided, update group and subgroup information
        if (savedLedger.getParentId() != null) {
            Optional<Ledger> parentLedger = ledgerRepository.findById(savedLedger.getParentId());
            if (parentLedger.isPresent()) {
                Ledger parent = parentLedger.get();
                if (parent.getGroup()) {
                    // Parent is a group
                    dto.setGroupId(parent.getId());
                    dto.setGroupName(parent.getLedgerName());
                } else if (parent.getSubGroup()) {
                    // Parent is a subgroup
                    dto.setSubGroupId(parent.getId());
                    dto.setSubGroupName(parent.getLedgerName());
                    // Find the group
                    Optional<Ledger> groupLedger = ledgerRepository.findById(parent.getParentId());
                    if (groupLedger.isPresent()) {
                        Ledger group = groupLedger.get();
                        dto.setGroupId(group.getId());
                        dto.setGroupName(group.getLedgerName());
                    }
                }
            }
        }

        return dto;
    }

    @Transactional
    public boolean updateLedger(Map<String, Object> request) {
        if (!request.containsKey("id") || request.get("id") == null) {
            return false;
        }
        Integer id = Integer.parseInt(request.get("id").toString());
        Optional<Ledger> optionalLedger = ledgerRepository.findById(id);
        if (optionalLedger.isEmpty()) {
            return false;
        }
        Ledger ledger = optionalLedger.get();

        // Update all fields if present in request
        if (request.containsKey("ledgerName")) ledger.setLedgerName((String) request.get("ledgerName"));
        if (request.containsKey("groupId")) ledger.setGroupId(request.get("groupId") != null ? Integer.parseInt(request.get("groupId").toString()) : null);
        if (request.containsKey("groupName")) ledger.setGroupName((String) request.get("groupName"));
        if (request.containsKey("subGroupId")) ledger.setSubGroupId(request.get("subGroupId") != null ? Integer.parseInt(request.get("subGroupId").toString()) : null);
        if (request.containsKey("subGroupName")) ledger.setSubGroupName((String) request.get("subGroupName"));
        if (request.containsKey("code")) ledger.setCode((String) request.get("code"));
        if (request.containsKey("version")) ledger.setVersion(request.get("version") != null ? Integer.parseInt(request.get("version").toString()) : null);
        if (request.containsKey("apVersion")) ledger.setApVersion(request.get("apVersion") != null ? Integer.parseInt(request.get("apVersion").toString()) : null);
        if (request.containsKey("ledgerHeader")) ledger.setLedgerHeader((String) request.get("ledgerHeader"));
        if (request.containsKey("isActive")) ledger.setActive(request.get("isActive") != null && Boolean.parseBoolean(request.get("isActive").toString()));
        if (request.containsKey("isDeleted")) ledger.setDeleted(request.get("isDeleted") != null && Boolean.parseBoolean(request.get("isDeleted").toString()));
        if (request.containsKey("isGroup")) ledger.setGroup(request.get("isGroup") != null && Boolean.parseBoolean(request.get("isGroup").toString()));
        if (request.containsKey("isLedger")) ledger.setLedger(request.get("isLedger") != null && Boolean.parseBoolean(request.get("isLedger").toString()));
        if (request.containsKey("isSubGroup")) ledger.setSubGroup(request.get("isSubGroup") != null && Boolean.parseBoolean(request.get("isSubGroup").toString()));
        if (request.containsKey("isEditable")) ledger.setEditable(request.get("isEditable") != null && Boolean.parseBoolean(request.get("isEditable").toString()));
        if (request.containsKey("isOptional")) ledger.setOptional(request.get("isOptional") != null && Boolean.parseBoolean(request.get("isOptional").toString()));
        if (request.containsKey("createdBy")) ledger.setCreatedBy(request.get("createdBy") != null ? Integer.parseInt(request.get("createdBy").toString()) : null);
        if (request.containsKey("updatedBy")) ledger.setUpdatedBy(request.get("updatedBy") != null ? Integer.parseInt(request.get("updatedBy").toString()) : null);
        if (request.containsKey("ledgerTypeId")) ledger.setLedgerTypeId(request.get("ledgerTypeId") != null ? Short.parseShort(request.get("ledgerTypeId").toString()) : null);
        if (request.containsKey("parentId")) ledger.setParentId(request.get("parentId") != null ? Integer.parseInt(request.get("parentId").toString()) : null);
        if (request.containsKey("tbMenuId")) ledger.setTbMenuId(request.get("tbMenuId") != null ? Integer.parseInt(request.get("tbMenuId").toString()) : null);
        if (request.containsKey("serialNumber")) ledger.setSerialNumber(request.get("serialNumber") != null ? Integer.parseInt(request.get("serialNumber").toString()) : null);
        if (request.containsKey("formula")) ledger.setFormula((String) request.get("formula"));
        if (request.containsKey("depreciationLedgerId")) ledger.setDepreciationLedgerId(request.get("depreciationLedgerId") != null ? Integer.parseInt(request.get("depreciationLedgerId").toString()) : null);
        if (request.containsKey("accumulatedDepreciationId")) ledger.setAccumulatedDepreciationId(request.get("accumulatedDepreciationId") != null ? Integer.parseInt(request.get("accumulatedDepreciationId").toString()) : null);
        if (request.containsKey("fsaAreaId")) ledger.setFsaAreaId(request.get("fsaAreaId") != null ? Short.parseShort(request.get("fsaAreaId").toString()) : null);
        if (request.containsKey("createdDate")) {
            Object val = request.get("createdDate");
            if (val != null) {
                try {
                    ledger.setCreatedDate(Timestamp.valueOf(val.toString().replace("T", " ").replace("Z", "")));
                } catch (Exception e) {
                    // ignore parse error
                }
            }
        }
        // Always update updatedDate to now
        ledger.setUpdatedDate(new Timestamp(System.currentTimeMillis()));
        // Save
        ledgerRepository.save(ledger);
        return true;
    }
}
