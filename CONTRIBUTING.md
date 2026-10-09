# Contributing Guidelines

Thank you for your interest in contributing to the **Campus Placement Drive Candidate Tracking & Shortlisting Portal**!

## How to Contribute

1. **Fork the Repository**:
   Click the **Fork** button at the top right of the GitHub page.

2. **Clone your Fork**:
   ```bash
   git clone https://github.com/<your-username>/campus-placement-portal.git
   cd campus-placement-portal
   ```

3. **Create a Feature Branch**:
   ```bash
   git checkout -b feature/your-feature-name
   ```

4. **Code Guidelines**:
   - Keep code **beginner-friendly** and well-commented.
   - Follow standard Java naming conventions (PascalCase for classes, camelCase for methods/variables).
   - Maintain the direct MVC pattern: `controller` -> `service` -> `repository` -> `model`.
   - Write or update unit tests in `src/test/java` for any new logic added.

5. **Run Tests**:
   Ensure all tests pass prior to committing:
   ```bash
   mvn test
   ```

6. **Submit a Pull Request**:
   Push your branch and open a PR with a concise description of your changes.
