# ExpenSMS

ExpenSMS is an Android application that parses SMS messages to track and manage expenses. It's built using Kotlin and Jetpack Compose, providing a modern and efficient user interface for expense tracking.

## Features

- Automatic parsing of SMS messages for expense information
- Categorization and display of expenses
- Calendar view for easy navigation of expenses by date
- Adaptive layout for different screen sizes
- Dark mode support

## Tech Stack

- Kotlin
- Jetpack Compose
- Room Database
- Hilt for dependency injection
- Proto DataStore
- WorkManager for background tasks
- Compose Calendar

## Getting Started

To get started with ExpenSMS, follow these steps:

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/ExpenSMS.git
   ```

2. Open the project in Android Studio.

3. Sync the project with Gradle files.

4. Run the app on an emulator or physical device.

## Project Structure

The project follows a standard Android app structure with the following key packages:

- `data`: Contains data models, database, and repositories
- `di`: Dependency injection modules
- `ui`: Compose UI components and screens
- `utils`: Utility classes and functions
- `worker`: Background workers for processing SMS messages

## Contributing

We welcome contributions to ExpenSMS! Here's how you can contribute:

1. Fork the repository.

2. Create a new branch for your feature or bug fix:
   ```
   git checkout -b feature/your-feature-name
   ```

3. Make your changes and commit them with a descriptive commit message.

4. Push your changes to your fork:
   ```
   git push origin feature/your-feature-name
   ```

5. Create a pull request to the main repository.

Please ensure your code follows the project's coding standards and includes appropriate tests.

## Building and Testing

To build the project:
```bash
./gradlew build
```

To run tests:
```bash
./gradlew test
```

# License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Contact

If you have any questions or suggestions, please open an issue on the GitHub repository.

Happy coding!
