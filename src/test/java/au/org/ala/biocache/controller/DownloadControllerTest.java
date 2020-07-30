package au.org.ala.biocache.controller;

import au.org.ala.biocache.service.AuthService;
import au.org.ala.biocache.service.DownloadService;
import au.org.ala.biocache.service.LoggerService;
import au.org.ala.biocache.web.DownloadController;
import au.org.ala.biocache.web.OccurrenceController;
import com.google.common.collect.ImmutableMap;
import junit.framework.TestCase;
import org.ala.client.model.LogEventVO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.http.HttpServletResponse;

import java.util.Arrays;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for occurrence services.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:springTest.xml"})
@WebAppConfiguration
public class DownloadControllerTest extends TestCase {

    static {
        System.setProperty("biocache.config", System.getProperty("user.dir") + "/src/test/resources/biocache-test-config.properties");
    }

    public final int TEST_INDEX_SIZE = 1000;
    public final int DEFAULT_SEARCH_PAGE_SIZE = 10;
    public final int INDEXED_FIELD_SIZE = 377;

    @Autowired
    DownloadController downloadController;

    @Autowired
    DownloadService downloadService;

    AuthService authService;

    @Autowired
    WebApplicationContext wac;

    MockMvc mockMvc;

    @Before
    public void setup() {

        authService = mock(AuthService.class);
        ReflectionTestUtils.setField(downloadController, "authService", authService);

        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    public void offlineDownloadValidEmailTest() throws Exception {

        when(authService.getUserDetails("test@test.com"))
                .thenReturn((Map)ImmutableMap.of(
                        "locked", false,
                        "roles", Arrays.asList("ROLE_USER")
                ));

        this.mockMvc.perform(get("/occurrences/offline/download*")
                .param("reasonTypeId", "10")
                .param("email", "test@test.com"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"));

        verify(authService).getUserDetails("test@test.com");
    }

    @Test
    public void downloadInvalidEmailTest() throws Exception {

        when(authService.getUserDetails("test@test.com"))
                .thenReturn(null);

        this.mockMvc.perform(get("/occurrences/offline/download*")
                .param("reasonTypeId", "10")
                .param("email", "test@test.com"))
                .andExpect(status().is4xxClientError());

        verify(authService).getUserDetails("test@test.com");
    }

    @Test
    public void downloadLockedEmailTest() throws Exception {

        when(authService.getUserDetails("test@test.com"))
                .thenReturn((Map)ImmutableMap.of(
                        "locked", true
                ));

        this.mockMvc.perform(get("/occurrences/offline/download*")
                .param("reasonTypeId", "10")
                .param("email", "test@test.com"))
                .andExpect(status().is4xxClientError());

        verify(authService).getUserDetails("test@test.com");
    }

    @Test
    public void downloadNoRoleEmailTest() throws Exception {

        when(authService.getUserDetails("test@test.com"))
                .thenReturn((Map)ImmutableMap.of(
                        "locked", false,
                        "roles", Arrays.asList("NO_ROLE")
                ));

        this.mockMvc.perform(get("/occurrences/offline/download*")
                .param("reasonTypeId", "10")
                .param("email", "test@test.com"))
                .andExpect(status().is4xxClientError());

        verify(authService).getUserDetails("test@test.com");
    }
}