package org.strategoxt.imp.testing.strategies;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.imp.testing.Activator;
import org.strategoxt.imp.testing.listener.ITestListener;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

public class testlistener_init_0_0 extends Strategy {

	public static testlistener_init_0_0 instance = new testlistener_init_0_0();

	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm current) {
		// Display.getDefault().syncExec(new Runnable() {
		// public void run() {
		// try {
		// IViewPart a =
		// PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("org.strategoxt.imp.testing.views.testrunviewpart");
		// // Using reflection, because if I use a cast, I get a ClassCastException
		// // cannot cast type <x> to <x>. Probably because of some different classloader issue.
		// Method m = a.getClass().getMethod("reset", new Class[] {}) ;
		// m.invoke(a);
		// } catch(Exception e) {
		// e.printStackTrace();
		// }
		// }
		// });
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(
				ITestListener.EXTENSION_ID);

		try {
			for (IConfigurationElement e : config) {
				final Object o = e.createExecutableExtension("class");
				ISafeRunnable runner = new ISafeRunnable() {

					public void run() throws Exception {
						// Using reflection, because if I use a cast, I get a ClassCastException
						// cannot cast type <x> to <x>. Probably because of some different classloader issue.
						Method m = o.getClass().getMethod("reset", new Class[] {});
						if (!Modifier.isAbstract(m.getModifiers())) {
							m.invoke(o);
						}
					}

					public void handleException(Throwable exception) {
						//
					}
				};
				SafeRunner.run(runner);
			}
			if(config.length == 0){
				Activator.getInstance().getLog().log(new Status(IStatus.INFO, Activator.kPluginID, "No TestListeners available to listen for test status"));
			}
		} catch (Exception cex) {
			Activator.getInstance().getLog().log(new Status(IStatus.ERROR, Activator.kPluginID, "Failed to notify listeners of updated test status. Maybe no listeners?", cex));
		}

		return current;
	}

}
