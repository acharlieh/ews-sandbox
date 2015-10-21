package info.huggard.charlie.ews;

import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.LocalDate;
import org.joda.time.MonthDay;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.PropertySet;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.service.folder.Folder;
import microsoft.exchange.webservices.data.core.service.item.Item;
import microsoft.exchange.webservices.data.core.service.schema.EmailMessageSchema;
import microsoft.exchange.webservices.data.core.service.schema.FolderSchema;
import microsoft.exchange.webservices.data.credential.ExchangeCredentials;
import microsoft.exchange.webservices.data.credential.WebCredentials;
import microsoft.exchange.webservices.data.search.FindItemsResults;
import microsoft.exchange.webservices.data.search.FolderView;
import microsoft.exchange.webservices.data.search.ItemView;
import microsoft.exchange.webservices.data.search.filter.SearchFilter;

public class LicenseDataExtractor {

	public static void main(String... args) throws Exception {
		ExchangeService service = new ExchangeService();
		ExchangeCredentials credentials = new WebCredentials("ch010485", args[0], "WHQ_NT_DOMAIN");
		service.setCredentials(credentials);
		service.setUrl(URI.create("https://email.cerner.com/EWS/Exchange.asmx"));
	
		FolderView view = new FolderView(1);
		Folder inbox = Folder.bind(service, WellKnownFolderName.Inbox);
		Folder splunk = inbox.findFolders(new SearchFilter.IsEqualTo(FolderSchema.DisplayName,"_Splunk"),view).iterator().next();
		Folder license = splunk.findFolders(new SearchFilter.IsEqualTo(FolderSchema.DisplayName,"License"),view).iterator().next();
		Folder daily = license.findFolders(new SearchFilter.IsEqualTo(FolderSchema.DisplayName,"Daily"),view).iterator().next();
		
		ItemView itemView = new ItemView(10);
		boolean more = true;
	
		DateTimeFormatter dtf  = DateTimeFormat.forPattern("d MMM");
		
		Map<LocalDate,Pair<LocalDate, String>> volumes = new TreeMap<LocalDate, Pair<LocalDate, String>>();
		
		while(more) {
			FindItemsResults<Item> items = daily.findItems(new SearchFilter.Exists(EmailMessageSchema.From),itemView);
			for(Item item:items) {
				item.load(PropertySet.FirstClassProperties);
				String body = item.getBody().toString();
				
				Element table = Jsoup.parse(body).select("table").last();
				
				Elements elems = table.select("tr > td:first-child > pre, tr > td:first-child + td > pre");
				Iterator<Element> iter = elems.iterator();
				LocalDate sentDate = LocalDate.fromDateFields(item.getDateTimeSent());
				
				while(iter.hasNext()) {
					String dateString = iter.next().html();
					String gb = iter.next().html(); 
					
					dateString = dateString.substring(5, dateString.length()).trim();
					
					MonthDay datePartial = MonthDay.parse(dateString,dtf);
										
					LocalDate reportDate;
					if (datePartial.getMonthOfYear() > sentDate.getMonthOfYear()) {
						reportDate = datePartial.toLocalDate(sentDate.getYear()-1); 
					}
					else {
						reportDate = datePartial.toLocalDate(sentDate.getYear());
					}
					
					if(volumes.containsKey(reportDate)) {
						Pair<LocalDate, String> data = volumes.get(reportDate);
						if(!data.getRight().equals(gb)) {
							System.err.printf("Ambigous data for %s Report %s => %s but Report %s => %s\n", reportDate, sentDate, gb, data.getLeft(), data.getRight());
						}
					} else {
					  volumes.put(reportDate, Pair.of(sentDate, gb));
					}
					
				}
			}
			more = items.isMoreAvailable();
			itemView.setOffset(itemView.getOffset()+itemView.getPageSize());
		}
		
		System.out.println("DATE,GB");
		for(Map.Entry<LocalDate,Pair<LocalDate, String>> report:volumes.entrySet()) {
			System.out.printf("%s,%s\n",report.getKey(),report.getValue().getRight());
		}	
	}
}
