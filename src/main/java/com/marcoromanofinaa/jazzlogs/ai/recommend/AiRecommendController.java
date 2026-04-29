package com.marcoromanofinaa.jazzlogs.ai.recommend;

import com.marcoromanofinaa.jazzlogs.curation.admin.AdminRequestAuthorizer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/ai/recommend")
@Slf4j
@RequiredArgsConstructor
public class AiRecommendController {

    private static final String ADMIN_HEADER = "X-Admin-Key";

    private final AdminRequestAuthorizer authorizer;
    private final AiRecommendService recommendService;

    @PostMapping
    public AiRecommendResponse recommend(
            @RequestHeader(ADMIN_HEADER) String adminKey,
            @Valid @RequestBody AiRecommendRequest request
    ) {
        authorizer.authorize(adminKey);
        log.info("Admin requested AI recommend question='{}'", request.question());
        return recommendService.recommend(request);
    }
}
