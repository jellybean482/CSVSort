## Java CSV Sort Reader

### Instructions

Create a simple application in the language of your choosing (preference for Java, HTML + Javascript, Python, or GoLang) <br>

The application should, at a minimum, do the following:<br>

1 Read a csv file with the following columns: Name, Address, Invoice Amount, Date of Sale<br>
2 Accept user input, either as a start parameter to the program or interactive in the application, to indicate which 
column to sort<br>
3 Using your own algorithm, don't use the sorting functions built into the language, provide a program that sorts the 
data by the given column<br>
4 Also allow sorting by two columns at once.<br>

Come to the interview prepared to discuss the program including how you performed the sort, alternate methods, and other general questions.<br>

Sample data:<br>

Name, Address, Invoice Amount, Date of Sale<br>
Doe, John, 123 Main St., 1275, 3/19/2021<br>
Smith, Jack, 75 Elm St., 750, 3/22/2021<br>
Jones, Jane, 559 5th Ave., 2250, 5/12/2020<br>
Brown, Brad, 123 Main St., 1890, 5/12/2021<br>

### Key Dependencies

- Java 17
- JUnit 5
- Eclipse 2024-03 (4.31.0)

### Build the Project

- Import the project into Eclipse IDE and build
- Or, use javac from the command line

### How to run the program

CSVSortReader [[column number][sort order] ...] csv_file_path<br>

Column number: starts at 0<br>
Sort order: a-acending (default); d-descending<br>

Example:<br>

CSVSortReader 0a 1d sort.csv<br>
The above command will sort file sort.csv primarily on column 0 in ascending order, secondarily on column 1 in descending order<br>
 
