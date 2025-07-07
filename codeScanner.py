import ast
import os
import git
import javalang
import re
from typing import List, Dict
import json

class DependencyScanner:
    def __init__(self, repo_path: str):
        self.repo_path = repo_path
        self.dsl_output = []
        self.details = {
            "files_scanned": 0,
            "lftp_transfers": [],
            "api_calls": []
        }

    def scan_repository(self) -> None:
        """Scan all Java, .NET, and Python files in the Git repository."""
        try:
            repo = git.Repo(self.repo_path)
            for file_path in repo.git.ls_files('*.py', '*.java', '*.cs').splitlines():
                full_path = os.path.join(self.repo_path, file_path)
                if file_path.endswith('.py'):
                    self.scan_python_file(full_path)
                elif file_path.endswith('.java'):
                    self.scan_java_file(full_path)
                elif file_path.endswith('.cs'):
                    self.scan_dotnet_file(full_path)
        except git.InvalidGitRepositoryError:
            self.details.setdefault("errors", []).append(f"Invalid Git repository: {self.repo_path}")
        except Exception as e:
            self.details.setdefault("errors", []).append(f"Error scanning repository: {str(e)}")

    def scan_python_file(self, file_path: str) -> None:
        """Scan a Python file for lftp and API calls."""
        try:
            with open(file_path, 'r', encoding='utf-8') as file:
                code = file.read()
                tree = ast.parse(code, filename=file_path)

            self.details["files_scanned"] += 1
            self._analyze_python_ast(tree, file_path)
        except Exception as e:
            self.details.setdefault("errors", []).append(f"Error in {file_path}: {str(e)}")

    def _analyze_python_ast(self, tree: ast.AST, file_path: str) -> None:
        """Analyze Python AST for lftp and API usage."""
        for node in ast.walk(tree):
            if isinstance(node, ast.Call) and isinstance(node.func, ast.Attribute):
                if (node.func.attr == 'run' and isinstance(node.func.value, ast.Name) and 
                    node.func.value.id == 'subprocess' and any('lftp' in arg.s.lower() for arg in node.args if isinstance(arg, ast.Str))):
                    lftp_cmd = next(arg.s for arg in node.args if isinstance(arg, ast.Str) and 'lftp' in arg.s.lower())
                    transfer_details = self._parse_lftp_command(lftp_cmd)
                    self.dsl_output.append({
                        "type": "lftp_transfer",
                        "details": transfer_details,
                        "file": file_path,
                        "line": node.lineno,
                        "language": "python"
                    })
                    self.details["lftp_transfers"].append(transfer_details)

                if (hasattr(node.func.value, 'id') and node.func.value.id == 'requests' and 
                    node.func.attr in ('get', 'post') and node.args and isinstance(node.args[0], ast.Str)):
                    api_url = node.args[0].s
                    self.dsl_output.append({
                        "type": "api_call",
                        "url": api_url,
                        "method": node.func.attr.upper(),
                        "file": file_path,
                        "line": node.lineno,
                        "language": "python"
                    })
                    self.details["api_calls"].append({"url": api_url, "method": node.func.attr.upper()})

    def scan_java_file(self, file_path: str) -> None:
        """Scan a Java file for lftp and API calls."""
        try:
            with open(file_path, 'r', encoding='utf-8') as file:
                code = file.read()
                tree = javalang.parse.parse(code)

            self.details["files_scanned"] += 1
            self._analyze_java_tree(tree, file_path, code)
        except Exception as e:
            self.details.setdefault("errors", []).append(f"Error in {file_path}: {str(e)}")

    def _analyze_java_tree(self, tree: javalang.tree.CompilationUnit, file_path: str, code: str) -> None:
        """Analyze Java parse tree for lftp and API usage."""
        for path, node in javalang.ast.walk_tree(tree):
            if isinstance(node, javalang.tree.MethodInvocation):
                if (node.qualifier == 'ProcessBuilder' and node.member == 'start' and 
                    any('lftp' in arg.value.lower() for arg in node.arguments if isinstance(arg, javalang.tree.Literal))):
                    lftp_cmd = next(arg.value for arg in node.arguments if isinstance(arg, javalang.tree.Literal) and 'lftp' in arg.value.lower())
                    transfer_details = self._parse_lftp_command(lftp_cmd)
                    self.dsl_output.append({
                        "type": "lftp_transfer",
                        "details": transfer_details,
                        "file": file_path,
                        "line": getattr(node, 'position', (0, 0))[0] or 0,
                        "language": "java"
                    })
                    self.details["lftp_transfers"].append(transfer_details)

                if (node.qualifier in ('HttpClient', 'HttpURLConnection') and 
                    node.member in ('get', 'post') and node.arguments and 
                    isinstance(node.arguments[0], javalang.tree.Literal)):
                    api_url = node.arguments[0].value.strip('"')
                    self.dsl_output.append({
                        "type": "api_call",
                        "url": api_url,
                        "method": node.member.upper(),
                        "file": file_path,
                        "line": getattr(node, 'position', (0, 0))[0] or 0,
                        "language": "java"
                    })
                    self.details["api_calls"].append({"url": api_url, "method": node.member.upper()})

    def scan_dotnet_file(self, file_path: str) -> None:
        """Scan a .NET (C#) file for lftp and API calls using regex."""
        try:
            with open(file_path, 'r', encoding='utf-8') as file:
                code = file.read()
                lines = code.splitlines()

            self.details["files_scanned"] += 1
            self._analyze_dotnet_code(code, file_path, lines)
        except Exception as e:
            self.details.setdefault("errors", []).append(f"Error in {file_path}: {str(e)}")

    def _analyze_dotnet_code(self, code: str, file_path: str, lines: List[str]) -> None:
        """Analyze .NET code for lftp and API calls using regex."""
        # Detect lftp in Process.Start
        lftp_pattern = r'Process\.Start\([^)]*lftp[^)]*\)'
        for match in re.finditer(lftp_pattern, code):
            line_num = code[:match.start()].count('\n') + 1
            # Extract command string (simplified)
            cmd_match = re.search(r'"([^"]*lftp[^"]*)"', match.group())
            if cmd_match:
                lftp_cmd = cmd_match.group(1)
                transfer_details = self._parse_lftp_command(lftp_cmd)
                self.dsl_output.append({
                    "type": "lftp_transfer",
                    "details": transfer_details,
                    "file": file_path,
                    "line": line_num,
                    "language": "csharp"
                })
                self.details["lftp_transfers"].append(transfer_details)

        # Detect HttpClient API calls
        api_pattern = r'HttpClient\(\)\.GetAsync\("([^"]+)"\)|HttpClient\(\)\.PostAsync\("([^"]+)"'
        for match in re.finditer(api_pattern, code):
            line_num = code[:match.start()].count('\n') + 1
            url = match.group(1) or match.group(2)
            method = "GET" if "GetAsync" in match.group() else "POST"
            self.dsl_output.append({
                "type": "api_call",
                "url": url,
                "method": method,
                "file": file_path,
                "line": line_num,
                "language": "csharp"
            })
            self.details["api_calls"].append({"url": url, "method": method})

    def _parse_lftp_command(self, cmd: str) -> Dict:
        """Parse lftp command for source, destination, and filename."""
        try:
            parts = cmd.split()
            file_index = parts.index('put') + 1 if 'put' in parts else -1
            filename = parts[file_index] if file_index > 0 and file_index < len(parts) else "unknown"
            host_index = next((i for i, part in enumerate(parts) if part.startswith('ftp://') or 'ftp' in part.lower()), -1)
            destination = parts[host_index] if host_index > 0 else "unknown"
            source = os.path.dirname(filename) or "local"
            return {"filename": filename, "source": source, "destination": destination}
        except Exception:
            return {"filename": "unknown", "source": "unknown", "destination": "unknown"}

    def generate_dsl(self, output_file: str) -> None:
        """Generate a DSL file from the scanned data."""
        dsl_content = self._create_dsl_content()
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write(dsl_content)

    def _create_dsl_content(self) -> str:
        """Create DSL content for lftp and API calls."""
        dsl_lines = ["# LFTP and API Insights DSL"]
        for item in self.dsl_output:
            if item["type"] == "lftp_transfer":
                details = item["details"]
                dsl_lines.append(
                    f"LFTP_TRANSFER {details['filename']} from {details['source']} to {details['destination']} "
                    f"at {item['file']}:{item['line']} ({item['language']})"
                )
            elif item["type"] == "api_call":
                dsl_lines.append(
                    f"API_CALL {item['method']} {item['url']} at {item['file']}:{item['line']} ({item['language']})"
                )
        return "\n".join(dsl_lines)

    def get_details(self) -> Dict:
        """Return scanning details."""
        return self.details

def main():
    scanner = DependencyScanner(".")
    scanner.scan_repository()
    scanner.generate_dsl("dependency_insights.dsl")
    print("DSL file generated: dependency_insights.dsl")
    print("Scan Details:")
    print(json.dumps(scanner.get_details(), indent=2))

if __name__ == "__main__":
    main()
