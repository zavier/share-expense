package com.github.zavier;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UnitTestBase {

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

}
