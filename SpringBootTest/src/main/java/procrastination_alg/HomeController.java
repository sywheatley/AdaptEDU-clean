package procrastination_alg;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    /**
     * Forward the root path to the single-page application entrypoint.
     *
     * This forwards `/` to `index2.html` so the SPA can take
     * over routing on the user side. Used as a bridge between the server and the static UI.
     *
     * @return a forward to the SPA HTML resource
     */
    @GetMapping("/")
    public String home() {
        return "forward:/index2.html";
    }
}
