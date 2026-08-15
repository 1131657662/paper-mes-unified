package com.paper.mes.ai.memory;

import com.paper.mes.ai.memory.dto.ProjectMemoryVersionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** Exposes retained snapshot metadata without returning historical document bodies. */
@Service
@RequiredArgsConstructor
public class ProjectMemoryVersionQueryService {

    private final ProjectMemoryDocumentRepository repository;

    public List<ProjectMemoryVersionResponse> versions() {
        return repository.findVersions().stream().map(this::response).toList();
    }

    private ProjectMemoryVersionResponse response(ProjectMemoryVersionRow row) {
        return new ProjectMemoryVersionResponse(row.docVersion(), row.schemaVersion(), row.checksum(), row.status(),
                row.patchNotes(), row.createdBy(), row.approvedBy(), row.createdAt());
    }
}
