package net.launcher;

import api.launcher.EventBus;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.jmx.MXNodeAlgorithm;
import com.sun.javafx.jmx.MXNodeAlgorithmContext;
import com.sun.javafx.sg.prism.NGNode;
import javafx.event.Event;
import javafx.scene.Node;

/**
 * @author ci010
 */
public class FXEventBus extends Node implements EventBus
{
	@Override
	protected NGNode impl_createPeer() {return null;}

	@Override
	public BaseBounds impl_computeGeomBounds(BaseBounds bounds, BaseTransform tx) {return null;}

	@Override
	protected boolean impl_computeContains(double localX, double localY) {return false;}

	@Override
	public Object impl_processMXNode(MXNodeAlgorithm alg, MXNodeAlgorithmContext ctx) {return null;}

	@Override
	public <T extends Event> T postEvent(T event)
	{
		Event.fireEvent(this, event);
		return event;
	}
}
