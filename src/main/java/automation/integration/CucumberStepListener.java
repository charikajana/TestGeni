package automation.integration;

import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EventHandler;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.PickleStepTestStep;
import io.cucumber.plugin.event.TestStepStarted;

/**
 * Listener to capture the current step name from Cucumber.
 * Register this in your Cucumber runner (e.g., @CucumberOptions(plugin = {"automation.integration.CucumberStepListener"}))
 */
public class CucumberStepListener implements ConcurrentEventListener {

    private static final ThreadLocal<String> currentStepName = new ThreadLocal<>();

    public static String getCurrentStepName() {
        return currentStepName.get();
    }

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestStepStarted.class, stepStartedHandler);
    }

    private final EventHandler<TestStepStarted> stepStartedHandler = event -> {
        if (event.getTestStep() instanceof PickleStepTestStep) {
            PickleStepTestStep pickleStep = (PickleStepTestStep) event.getTestStep();
            // This retrieves the actual step text with data injected (Scenario Outlines)
            currentStepName.set(pickleStep.getStep().getText());
        }
    };
}
