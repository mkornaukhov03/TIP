#!/usr/bin/python3
import os

from common import run_tests


def get_all_examples():
    return ["examples/" + x for x in os.listdir("examples")]


if __name__ == "__main__":
    options = "-types"
    negative = ["examples/err_doubledecl2.tip",
                "examples/record3.tip",
                "examples/err_unify2.tip",
                "examples/err_funass.tip",
                "examples/err_cmpfunc.tip",
                "examples/err_cmpfunc2.tip",
                "examples/ex5.tip",
                "examples/err_funass2.tip",
                "examples/record6.tip",
                "examples/err_doubledecl1.tip",
                "examples/err_assignfunc.tip",
                "examples/ex1.tip",
                "examples/err_notlocal2.tip",
                "examples/err_cmpfunc3.tip",
                "examples/err_notdecl.tip",
                "examples/parsing.tip",
                "examples/err_unify1.tip",
                "examples/err_locals.tip",
                "examples/err_decl_use.tip",
                "examples/err_notlocal.tip"]
    positive = list(filter(lambda x: x not in negative, get_all_examples()))

    run_tests(positive=positive, negative=negative, options=options)
