package cloud.cholewa.heating.infrastructure.error;

import cloud.cholewa.commons.error.model.ErrorId;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

//The names are the codes of the error responses, and callers branch on them: the web dashboard
//tells "no such room" from a 404 of the routing by NOT_FOUND_ROOM. A rename compiles and passes
//every other test here, so the names are written out once more - a failure of this test is a
//breaking change of the API, to be made together with the callers, not a test to adjust.
class HeatingErrorIdTest {

    @Test
    void should_keep_the_codes_callers_rely_on() {
        assertThat(Arrays.stream(HeatingErrorId.values()).map(ErrorId::codeOf))
            .containsExactlyInAnyOrder("NOT_FOUND_ROOM");
    }
}
