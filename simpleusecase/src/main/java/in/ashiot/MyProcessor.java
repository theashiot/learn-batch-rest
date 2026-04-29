package in.ashiot;

import jakarta.batch.api.chunk.ItemProcessor;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;

@Dependent
@Named("MyProcessor")
public class MyProcessor implements ItemProcessor {
    public MyProcessor() {}

    @Override
    public Object processItem(Object obj) throws Exception {
        String line = (String) obj;
        //Simulate processing delay
        Thread.sleep(5);
        return line.toUpperCase();
    }
}
