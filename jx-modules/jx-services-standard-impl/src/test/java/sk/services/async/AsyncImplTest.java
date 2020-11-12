package sk.services.async;

import org.junit.Test;
import sk.utils.statics.Cc;

import static org.junit.Assert.fail;

public class AsyncImplTest {
    IAsync async = new AsyncImpl();


    @Test
    public void runParallelTest() {
        try {
            async.runParallel(Cc.l(
                    () -> {},
                    () -> {throw new RuntimeException();},
                    () -> {}
            ));
            fail();
        } catch (Exception e) { }
    }

    @Test
    public void supplyParallelTest() {
        try {
            async.supplyParallel(Cc.l(
                    () -> 1,
                    () -> {throw new RuntimeException();},
                    () -> 2
            ));
            fail();
        } catch (Exception e) { }
    }
}