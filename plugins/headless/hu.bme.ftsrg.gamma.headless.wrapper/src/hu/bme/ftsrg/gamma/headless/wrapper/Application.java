package hu.bme.ftsrg.gamma.headless.wrapper;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
public class Application implements IApplication{

	@Override
	public Object start(IApplicationContext context) throws Exception {
		//System.out.print("Testing");
		//XtextStandaloneSetup.doSetup();

		CodeGenerator cd = new CodeGenerator();
		  String [] args = (String [])context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
		  if(args.length < 2) {
			  System.out.println("Arguments must be given!");
			  return null;
		  }
		  String pathOfFile = args[0];
		  String projectName = args[1];
		 Object obj = cd.execute(pathOfFile,projectName); 
		
		return null;
		
		
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}

}