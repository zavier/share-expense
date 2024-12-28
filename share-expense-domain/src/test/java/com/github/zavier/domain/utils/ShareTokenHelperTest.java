package com.github.zavier.domain.utils;

import org.junit.Test;

import static com.github.zavier.domain.utils.ShareTokenHelper.*;
import static org.junit.Assert.*;

public class ShareTokenHelperTest {

    @Test
    public void test() {
        String token = "java@test.com";

        final String shareTokenBody = generateShareToken(token);
        final boolean b = validateShareToken(shareTokenBody);
        assertTrue(b);
        final String shareTokenBody1 = getShareTokenBody(shareTokenBody);
        assertEquals(token, shareTokenBody1);
    }

}