package com.marcoromanofinaa.jazzlogs.user.jazzpreferences.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.marcoromanofinaa.jazzlogs.core.exception.FeatureUnavailableException;
import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.dto.UserJazzPreferencesOptionsDto;
import com.marcoromanofinaa.jazzlogs.user.mapper.UserMapper;
import com.marcoromanofinaa.jazzlogs.user.repository.UserRepository;
import com.marcoromanofinaa.jazzlogs.user.subscription.service.UserSubscriptionService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserJazzPreferencesServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserGraphPreferencesService userGraphPreferencesService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserSubscriptionService userSubscriptionService;

    private UserJazzPreferencesService service;

    @BeforeEach
    void setUp() {
        service = new UserJazzPreferencesService(
                userRepository,
                userGraphPreferencesService,
                userMapper,
                userSubscriptionService
        );
    }

    @Test
    void getPreferencesOptionsFailsWhenGraphVocabularyIsNotReady() {
        when(userGraphPreferencesService.getPreferencesOptions()).thenReturn(new UserJazzPreferencesOptionsDto(
                List.of(),
                List.of("Hard Bop"),
                List.of("Warm"),
                List.of("Piano"),
                List.of("Low"),
                List.of("Open to explore"),
                List.of("Intermediate")
        ));

        assertThatThrownBy(service::getPreferencesOptions)
                .isInstanceOf(FeatureUnavailableException.class)
                .hasMessageContaining("Jazz preference options are not available yet");
    }

    @Test
    void getPreferencesOptionsReturnsOptionsWhenAllRequiredCatalogsArePresent() {
        var options = new UserJazzPreferencesOptionsDto(
                List.of("Chet Baker"),
                List.of("Hard Bop"),
                List.of("Warm"),
                List.of("Piano"),
                List.of("Low"),
                List.of("Open to explore"),
                List.of("Intermediate")
        );
        when(userGraphPreferencesService.getPreferencesOptions()).thenReturn(options);

        assertThat(service.getPreferencesOptions()).isEqualTo(options);
    }
}
