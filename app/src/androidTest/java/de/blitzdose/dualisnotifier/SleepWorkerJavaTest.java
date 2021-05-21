package de.blitzdose.dualisnotifier;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.work.ListenableWorker;
import androidx.work.testing.TestWorkerBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
public class SleepWorkerJavaTest {
    private Context context;
    private Executor executor;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        executor = Executors.newSingleThreadExecutor();
    }

    @Test
    public void testSleepWorker() {
        BackgroundWorker worker =
                (BackgroundWorker) TestWorkerBuilder.from(context,
                        BackgroundWorker.class,
                        executor)
                        .build();

        ListenableWorker.Result result = worker.doWork();
        assertThat(result, is(ListenableWorker.Result.success()));
    }
}