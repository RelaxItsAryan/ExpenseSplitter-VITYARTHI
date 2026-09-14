# Expense Splitter

<img width="1712" height="1118" alt="image" src="https://github.com/user-attachments/assets/f3f4b3c6-0028-4b62-800c-eac939c7a78c" />


Expense Splitter is a small Java application for managing shared group expenses. It calculates each member's balance and shows which members owe money or should receive money.

The project provides two interfaces:

1. **Web application** - an HTML/CSS interface served by a Java backend.
2. **Console application** - a command-line interface provided by `ExpenseSplitter.java`.

## Features

- Add members to an expense group.
- Add expenses and select who paid.
- Select which members participated in each expense.
- Split each expense equally among the selected participants.
- View all recorded expenses.
- View each member's balance.
- Calculate a settlement plan in the console application.
- Use a custom background image in the web application.
- Use the application without external Java libraries or a database.

## Project files

```text
ExpenseSplitter.java      Console version
ExpenseSplitterWeb.java   Java HTTP server and HTML rendering
style.css                 Web application styling
README.md                 Project documentation
```

## Requirements

Install the following before running the project:

- Java Development Kit (JDK) 17 or newer
- A terminal such as PowerShell, Command Prompt, or a Unix shell
- A web browser for the web application

Java 17 or newer is required because the project uses modern Java features including records, text blocks, and arrow-style switch cases.

Verify that Java is installed:

```powershell
java -version
javac -version
```

Both commands should report version 17 or newer.

## Setup

### 1. Download or clone the project

If the project is hosted on GitHub, clone it with:

```powershell
git clone https://github.com/YOUR_USERNAME/YOUR_REPOSITORY.git
```

Replace `YOUR_USERNAME` and `YOUR_REPOSITORY` with the repository owner's username and repository name.

Move into the project directory:

```powershell
cd "java project"
```

If the project was downloaded as a ZIP file, extract it and open a terminal in the extracted project folder.

### 2. Check the required files

The project directory should contain:

```text
ExpenseSplitter.java
ExpenseSplitterWeb.java
style.css
```


## Running the web application

The web application uses Java's built-in HTTP server. No Maven, Gradle, Node.js, npm package, or external dependency is required.

### 1. Compile the web backend

From the project directory, run:

```powershell
javac ExpenseSplitterWeb.java
```

### 2. Start the web server

```powershell
java ExpenseSplitterWeb
```

The server will start on port `8080` and display:

```text
Expense Splitter is running at http://localhost:8080
```

### 3. Open the application

Open this address in a browser:

```text
http://localhost:8080
```

### 4. Use the application

1. Add at least two members using the **Add member** form.
2. Enter an expense description.
3. Enter the total amount.
4. Select the person who paid.
5. Check the members who need to pay for that expense.
6. Uncheck members who did not participate.
7. Click **Add expense**.
8. Review the members, balances, and expense history.

The amount is split equally between the selected participants. For example, a `$30.00` expense with two selected participants gives each participant a `$15.00` share. Members who are not selected do not receive a share of that expense.

### Stop the web server

Press `Ctrl+C` in the terminal running the server.

## Running the console application

The console version provides additional settlement-plan output.

### 1. Compile the console application

```powershell
javac ExpenseSplitter.java
```

### 2. Start the console application

```powershell
java ExpenseSplitter
```

### 3. Console menu

The console application provides these options:

```text
1. Add member
2. Add expense
3. View expenses
4. View balances
5. View settlement plan
6. Exit
```

Follow the prompts shown in the terminal.

## Configuration

### Web server port

The web server listens on port `8080` by default. This value is configured in `ExpenseSplitterWeb.java`:

```java
HttpServer.create(new InetSocketAddress(8080), 0);
```

To use another available port, change `8080` to the desired port, recompile, and restart the application.

For example, after changing the port to `9090`, open:

```text
http://localhost:9090
```

## Data storage and limitations

- Data is stored in memory only.
- Members and expenses are cleared when the application stops.
- The web server is intended for local use and does not include user accounts or authentication.
- The web version currently splits expenses equally among selected participants.
- The console version uses integer cents to avoid common floating-point money errors.
- There is no database or permanent file storage.
- Only one running instance should use port `8080` at a time.

## Compiled files

Compiling the project creates `.class` files. These are generated files and should not be committed to Git:

```gitignore
*.class
```

If necessary, remove generated files before pushing the project:

```powershell
Remove-Item *.class
```

## Troubleshooting

### `javac` is not recognized

Install a JDK, not only a Java Runtime Environment, and ensure the JDK `bin` directory is included in the system `PATH`.

Then open a new terminal and run:

```powershell
java -version
javac -version
```

### Address already in use

Another program is already using port `8080`. Stop the other program or change the port in `ExpenseSplitterWeb.java`.

### The background image is not visible

Confirm that:

1. It is in the same directory as `ExpenseSplitterWeb.java`.
2. The server was started from the project directory.
3. The browser cache was refreshed with `Ctrl+F5`.

### The stylesheet is not visible

Confirm that `style.css` is in the same directory from which the Java server was started. The server reads and serves the stylesheet from the current working directory.

## License

This project is intended for learning and personal use. Add a project-specific license here if the repository will be distributed publicly.
