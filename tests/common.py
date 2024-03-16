import subprocess
from typing import List
import multiprocessing
import termcolor
import tabulate

# There is a sbt file lock =(
MAX_PROC = 1

PATH_TO_TIP = "./tip"


def _exit_code_to_str(code: int) -> str:
    if code == 0:
        return termcolor.colored("success", "green")
    else:
        return termcolor.colored("failure", "red")


def _worker(arg):
    (path_to_file, options) = arg
    cmd = f"{PATH_TO_TIP} {options} {path_to_file}"
    print(f"Running cmd: \"{cmd}\"")  # comment out if there are too many logs
    try:
        result = subprocess.run(cmd.split(" "), capture_output=True)
        exit_code = result.returncode
    except subprocess.CalledProcessError as e:
        exit_code = e.returncode
        print(f"> Error with {path_to_file}")

    return exit_code


def run_tests(positive: List[str], negative: List[str], options: str):
    all_tests = positive + negative
    expected_exit_codes = [0 for _ in positive] + [1 for _ in negative]

    with multiprocessing.Pool(processes=MAX_PROC) as pool:
        exit_codes = pool.map(_worker, [(path, options) for path in all_tests])
        assert len(all_tests) == len(exit_codes)
        failed_tests = []
        for i in range(len(exit_codes)):
            if expected_exit_codes[i] != exit_codes[i]:
                failed_tests.append(i)

        print(
            termcolor.colored("{}/{} tests passed".format(len(all_tests) - len(failed_tests), len(all_tests)),
                              "cyan"))
        if len(failed_tests) == 0:
            print(termcolor.colored("OK", "green"), end="\n\n")
        else:

            print(termcolor.colored("FAILED", "red"), end="\n\n")
            table = [
                ["filename",
                 "expected",
                 "obtained"]
            ]
            for idx in failed_tests:
                table.append([
                    all_tests[idx],
                    _exit_code_to_str(expected_exit_codes[i]),
                    _exit_code_to_str(exit_codes[idx])
                ])
            print(tabulate.tabulate(table, headers="firstrow", tablefmt="github"))
