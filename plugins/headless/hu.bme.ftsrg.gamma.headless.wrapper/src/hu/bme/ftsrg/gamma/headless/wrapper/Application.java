package hu.bme.ftsrg.gamma.headless.wrapper;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
public class Application implements IApplication{

	@Override
	public Object start(IApplicationContext arg0) throws Exception {
		//System.out.print("Testing");
		//XtextStandaloneSetup.doSetup();

		CodeGenerator cd = new CodeGenerator();
		
		Object obj = cd.execute(); 
		
		return null;
		
		
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}

}