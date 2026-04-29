package in.ashiot;

import jakarta.batch.api.chunk.ItemProcessor;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;

@Dependent
@Named("LogItemProcessor")
public class LogItemProcessor implements ItemProcessor {

    @Override
    public Object processItem(Object item) throws Exception {
        System.out.println("MESSAGE FROM THE APP: Processing Items");
        String line = (String) item;
        String processedLine = line.replaceAll("[{,}]", "\n");
        return processedLine;
    }
    
}
