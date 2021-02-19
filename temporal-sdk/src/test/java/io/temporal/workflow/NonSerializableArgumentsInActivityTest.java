/*
 *  Copyright (C) 2020 Temporal Technologies, Inc. All Rights Reserved.
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package io.temporal.workflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.activity.LocalActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.testing.TestWorkflowRule;
import java.time.Duration;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class NonSerializableArgumentsInActivityTest {

  private final WorkflowTest.NonDeserializableExceptionActivityImpl activitiesImpl =
      new WorkflowTest.NonDeserializableExceptionActivityImpl();

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(TestNonSerializableArgumentsInActivityWorkflow.class)
          .setActivityImplementations(activitiesImpl)
          .setUseExternalService(Boolean.parseBoolean(System.getenv("USE_DOCKER_SERVICE")))
          .setTarget(System.getenv("TEMPORAL_SERVICE_ADDRESS"))
          .build();

  @Test
  public void testNonSerializableArgumentsInActivity() {
    WorkflowTest.TestWorkflow1 workflowStub =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                WorkflowTest.TestWorkflow1.class,
                WorkflowTest.newWorkflowOptionsBuilder(testWorkflowRule.getTaskQueue()).build());

    String result = workflowStub.execute(testWorkflowRule.getTaskQueue());
    Assert.assertEquals(
        "ApplicationFailure-io.temporal.common.converter.DataConverterException", result);
  }

  public static class TestNonSerializableArgumentsInActivityWorkflow
      implements WorkflowTest.TestWorkflow1 {

    @Override
    public String execute(String taskQueue) {
      StringBuilder result = new StringBuilder();
      ActivityStub activity =
          Workflow.newUntypedActivityStub(
              ActivityOptions.newBuilder()
                  .setScheduleToCloseTimeout(Duration.ofSeconds(5))
                  .build());
      ActivityStub localActivity =
          Workflow.newUntypedLocalActivityStub(
              LocalActivityOptions.newBuilder()
                  .setScheduleToCloseTimeout(Duration.ofSeconds(5))
                  .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(1).build())
                  .build());
      try {
        activity.execute("Execute", Void.class, "boo");
      } catch (ActivityFailure e) {
        result.append(e.getCause().getClass().getSimpleName());
      }
      result.append("-");
      try {
        localActivity.execute("Execute", Void.class, "boo");
      } catch (ActivityFailure e) {
        result.append(((ApplicationFailure) e.getCause()).getType());
      }
      return result.toString();
    }
  }

  public class NonDeserializableExceptionActivityImpl
      implements WorkflowTest.NonDeserializableArgumentsActivity {

    @Override
    public void execute(int arg) {}
  }
}
