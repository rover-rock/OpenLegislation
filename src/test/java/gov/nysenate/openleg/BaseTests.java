package gov.nysenate.openleg;

import gov.nysenate.openleg.config.DatabaseConfig;
import gov.nysenate.openleg.config.WebApplicationConfig;
import gov.nysenate.openleg.util.Application;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {WebApplicationConfig.class})
@ActiveProfiles("test")
public abstract class BaseTests
{
    protected MockMvc mockMvc;

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired
    protected WebApplicationContext wac;

    @BeforeClass
    public static void bootstrap() {
        Application.bootstrap(Application.TEST_PROPERTY_FILENAME);
    }

    @Before
    public void setup() {
        this.mockMvc = webAppContextSetup(this.wac).build();
    }
}