package com.pulse.auth.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratedAvatarTest {

    private static String svgFor(String alias) {
        return new String(GeneratedAvatar.forAlias(alias), StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("the disc shows the uppercase initial of the alias")
    void showsInitial() {
        assertThat(svgFor("marilo")).contains("<svg", ">M<");
    }

    @Test
    @DisplayName("the same alias always gets the same colour, different aliases differ")
    void colourIsDeterministic() {
        assertThat(svgFor("marilo")).isEqualTo(svgFor("marilo"));
        assertThat(svgFor("marilo")).isNotEqualTo(svgFor("cgomez"));
    }

    @Test
    @DisplayName("aliases that would break the XML fall back to a question mark")
    void nonAlphanumericInitialIsSafe() {
        assertThat(svgFor("<script>")).contains(">?<").doesNotContain("<script>");
        assertThat(svgFor("")).contains(">?<");
    }
}
