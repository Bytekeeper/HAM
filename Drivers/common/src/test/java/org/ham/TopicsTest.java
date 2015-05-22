package org.ham;

import junit.framework.TestCase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by dante on 22.05.15.
 */
public class TopicsTest {
    @Test
    public void shouldMatchIdentity()
    {
        // GIVEN

        // WHEN
        boolean matches = Topics.matches("test/test2", "test/test2");

        // THEN
        assertThat( matches, is( true ));
    }

    @Test
    public void shouldMatchWildcardOnly()
    {
        // GIVEN

        // WHEN
        boolean matches = Topics.matches("test", "#");

        // THEN
        assertThat( matches, is( true ));
    }

    @Test
    public void shouldMatchWildcardAtEnd()
    {
        // GIVEN

        // WHEN
        boolean matches = Topics.matches("test/test", "test/#");

        // THEN
        assertThat( matches, is( true ));
    }

    @Test
    public void shouldMatchSubtopicWildcardAtEnd()
    {
        // GIVEN

        // WHEN
        boolean matches = Topics.matches("test/test2/test3", "test/#");

        // THEN
        assertThat( matches, is( true ));
    }

    @Test
    public void shouldMatchPlusWildcardAtBeginning()
    {
        // GIVEN

        // WHEN
        boolean matches = Topics.matches("test/test2/test3", "+/test2/test3");

        // THEN
        assertThat( matches, is( true ));
    }

    @Test
    public void shouldMatchPlusWildcardInMiddle()
    {
        // GIVEN

        // WHEN
        boolean matches = Topics.matches("test/test2/test3", "test/+/test3");

        // THEN
        assertThat( matches, is( true ));
    }

    @Test
    public void shouldMatchPlusWildcardAtEnd()
    {
        // GIVEN

        // WHEN
        boolean matches = Topics.matches("test/test2/test3", "test/test2/+");

        // THEN
        assertThat( matches, is( true ));
    }
}