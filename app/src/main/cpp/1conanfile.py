import os
import subprocess

from conan import ConanFile
from conan.tools.cmake import CMakeToolchain, CMake


class FPTNLibrary(ConanFile):
    name = "fptn"
    settings = (
        "os",
        "arch",
        "compiler",
        "build_type",
    )
    generators = (
#         "CMakeToolchain",
        "CMakeDeps",
    )

    requires = ("zlib/1.3.1",)
    default_options = {
        "fptn/*:build_only_fptn_lib": True,
        "fptn/*:with_gui_client": False,
    }

    def requirements(self):
        self._register_fptn()
        self.requires("fptn/0.0.0@local/local", override=True, force=True)

    def generate(self):
        tc = CMakeToolchain(self)
        tc.generate()

    def build(self):
        cmake = CMake(self)
        cmake.configure()
        cmake.build()

    def config_options(self):
        if self.settings.os == "Windows":
            self.options.rm_safe("fPIC")

    def _register_fptn(self):
        script_dir = os.path.dirname(os.path.abspath(__file__))
        recipe_rel_path = os.path.join(script_dir, ".conan", "recipes", "fptn")
        print("========" * 100)
        print(
            [
                "conan",
                "export",
                recipe_rel_path,
                "--name=fptn",
                "--version=0.0.0",
                "--user=local",
                "--channel=local",
            ]
        )
