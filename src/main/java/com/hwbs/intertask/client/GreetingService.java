package com.hwbs.intertask.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.hwbs.intertask.shared.ProgressState;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("greet")
public interface GreetingService extends RemoteService {
  boolean generate()       throws IllegalStateException;
  ProgressState progress() throws IllegalStateException;

  // debug: funny-dummy mockups
  boolean generateDummy()       throws IllegalStateException;
  ProgressState progressDummy() throws IllegalStateException;
}
