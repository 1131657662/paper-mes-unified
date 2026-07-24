package com.paper.mes.processorder.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务（P2-4）：破损图片本地落盘。
 */
public interface FileStorageService {

    /**
     * 校验并保存单个图片文件，返回可对外访问的 URL 相对路径
     * （形如 {urlPrefix}/damage/yyyyMMdd/UUID.ext）。
     * 校验项：非空、扩展名白名单(jpg/jpeg/png/webp/gif)、大小≤10MB。违规抛 BusinessException。
     */
    String store(MultipartFile file);

    /** 删除由本服务生成的文件 URL；路径越界或删除失败时抛出业务异常。 */
    void delete(String fileUrl);
}
