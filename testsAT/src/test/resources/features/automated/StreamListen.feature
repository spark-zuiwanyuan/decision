@api
Feature: Stream listen
	listenStream method should do something cool

	Scenario Outline: Listen to an existing stream
		Given I drop every existing stream		
		When I create a stream with name '<streamName>' and columns (with type):
			| 1  | String  |
			| 2  | Integer |
		Then the number of kafka topics is, at least, '2'
		When I listen to a stream with name '<streamName>'			
		And I insert into a stream with name '<streamName>' this data:
			| 1 | a | 
			| 2 | 4 |
		And I insert into a stream with name '<streamName>' this data:
			| 1 | b | 
			| 2 | 5 |
		And I insert into a stream with name '<streamName>' this data:
			| 1 | a | 
			| 2 | 4 |
			| badcol | 3 |
		And I insert into a stream with name '<streamName>' this data:
			| badcol1 | a | 
			| badcol2 | 4 |
						
		And I wait '5' seconds
		And the stream '<streamName>' has 'LISTEN' as active actions
		Then the number of kafka topics is '4'
		Then the stream '<streamName>' has this content (with column name, type and value):
			| 1 String a | 2 Integer 4.0 |
			| 1 String b | 2 String 5.0 |
 			
		Examples:
			 | streamName 	| 
			 | testStream 	|

	Scenario Outline: Listen to an non-existing stream
		Given I drop every existing stream				
		When I listen to a stream with name '<streamName>'		
		Then an exception 'IS' thrown with class 'StratioEngineOperationException' and message like 'Stream <streamName> does not exists'			
			 
		Examples:
			 | streamName 	| 
			 | testStream 	|
			 
	@ignore
	Scenario Outline: Listen to a stream, named as an internal topic
		Given I drop every existing stream				
		When I create a stream with name '<streamName>' and columns (with type):
			| 1  | String  |
			| 2  | Integer |
		Then an exception 'IS' thrown with class 'XX' and message like 'YY'			
			 
		Examples:
			 | streamName 	              | 
			 | stratio_streaming_data     |
			 | stratio_streaming_requests |
			 
	Scenario Outline: Listen to an bad named stream
		Given I drop every existing stream				
		When I listen to a stream with name '<streamName>'		
		Then an exception 'IS' thrown with class 'StratioAPISecurityException' and message like '<message>'			
			 
		Examples:
			 | streamName 	| message                     |
			 |          	| Stream name cannot be empty |
			 | //NULL// 	| Stream name cannot be null  | 
			 
	Scenario Outline: Listening to streams with special names should be forbidden
		Given I drop every existing stream
		When I create a stream with name '<streamName>' and columns (with type):
			| 1  | String  |
			| 2  | Integer | 				
		When I listen to a stream with name '<streamName>'		
		Then the number of kafka topics is, at least, '3'
		And an exception 'IS' thrown with class 'StratioAPISecurityException' and message like 'Stream name .*? is not compatible with LISTEN action'
		
		Examples:
			 | streamName   |  
			 | "            |  
			 | `            | 
			 | 0x0008       |
			 |  korean: 향                        | 
			 | cyrilic: ᴞ            |			
			 | japanese: 強           | 
			 | viet: 漢           | 			 
			 | arab: أنا أحب القراءة كثيرا |
			 