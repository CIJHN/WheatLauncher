package net.launcher;

import net.launcher.api.EventBus;
import net.launcher.api.LauncherInitEvent;
import org.junit.Test;

/**
 * @author ci010
 */
public class FXEventBusTest
{
	@Test
	public void postEvent() throws Exception
	{
		EventBus bus = new FXEventBus();
		bus.addEventHandler(LauncherInitEvent.LAUNCHER_INIT, event ->
		{
			System.out.println("event!");
		});
	}

}
