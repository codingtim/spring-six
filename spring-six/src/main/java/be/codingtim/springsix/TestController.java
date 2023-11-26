package be.codingtim.springsix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

@Controller
@RequestMapping("/test")
public class TestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestController.class);

    private final RestTemplate restTemplate;

    public TestController(RestTemplate restTemplate) {
        LOGGER.info("running");
        this.restTemplate = restTemplate;
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<String> getPage(@RequestParam("page") int page) {
        String result = restTemplate.getForObject("https://www.rfc-editor.org/rfc/rfc7643#page-" + page, String.class);
        return ResponseEntity.ok(result);
    }

}
