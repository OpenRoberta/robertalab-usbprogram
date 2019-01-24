package de.fhg.iais.roberta.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.condition.OS.WINDOWS;

class UtilsTests {
    @Test
    void generateToken_ShouldReturnToken_WhenRun() {
        String token = OraTokenGenerator.generateToken();

        assertThat(token.isEmpty(), is(false));
        assertThat(token, is(notNullValue()));
    }

    // there is a chance for this test to fail, but it should be pretty slim with the 34^8 possibilities
    @Test
    void generateToken_ShouldGenerallyReturnDifferentTokens_WhenRunMultipleTimes() {
        Collection<String> tokens = new ArrayList<>();
        for ( int i = 0; i < 100; i++ ) {
            tokens.add(OraTokenGenerator.generateToken());
        }

        Collection<String> allItems = new HashSet<>();
        Set<String> duplicates = tokens.stream().filter(n -> !allItems.add(n)) //Set.add() returns false if the item was already in the set.
            .collect(Collectors.toSet());

        assertThat(duplicates, empty());
    }

    // TODO implement properly on Windows
    @EnabledOnOs(WINDOWS)
    @Test
    void getWMIValue_ShouldReturnQueryResult_WhenGivenQueryAndField() {
        try {
            String result = JWMI.getWMIValue("SELECT * FROM Win32_PnPEntity", "Caption");

            assertThat(result, is(notNullValue()));
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    @Test
    void getProperty_ShouldReturnProperty_WhenORPropsWereLoaded() {
        PropertyHelper instance = PropertyHelper.getInstance();

        assertThat(instance, notNullValue());

        String groupId = instance.getProperty("groupId");
        assertThat(groupId, is("de.fhg.iais.roberta"));

        String artifactId = instance.getProperty("artifactId");
        assertThat(artifactId, is("OpenRobertaUSB"));
    }
}
