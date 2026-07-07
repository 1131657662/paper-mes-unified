package com.paper.mes.file;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@RequirePermission(Permissions.ORDER_VIEW)
public class ProtectedFileController {

    private static final String API_FILES_PREFIX = "/api/files/";

    private final ProtectedFileService protectedFileService;

    @GetMapping("/**")
    public ResponseEntity<Resource> read(HttpServletRequest request) {
        ProtectedFileService.ProtectedFile file = protectedFileService.load(relativePath(request));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(file.resource());
    }

    private String relativePath(HttpServletRequest request) {
        String prefix = request.getContextPath() + API_FILES_PREFIX;
        String uri = request.getRequestURI();
        return uri.startsWith(prefix) ? uri.substring(prefix.length()) : "";
    }
}
