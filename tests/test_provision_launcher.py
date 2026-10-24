"""Verify certificate discovery address and Android loopback routing remain separate."""
import contextlib
import hashlib
import io
import json
from pathlib import Path
import runpy
import shlex
import subprocess
import sys
import tempfile
import unittest
from unittest import mock

SCRIPT = Path(__file__).resolve().parents[1] / 'tools/provision_launcher.py'
PIN = 'ab' * 32
CERTIFICATE = b'test certificate, not a real TV credential'

class ProvisioningTests(unittest.TestCase):
    def exercise(self, status, *, replace=False, success=True):
        calls = []
        def run(argv, **kwargs):
            calls.append(argv)
            if len(calls) == 1:
                return subprocess.CompletedProcess(argv, 0, stdout=status, stderr='')
            return subprocess.CompletedProcess(argv, 0, stdout='Bundle[{configured=true}]' if success else 'Bundle[{configured=false}]', stderr='')
        connection = mock.MagicMock()
        connection.sock.getpeercert.return_value = CERTIFICATE
        with tempfile.TemporaryDirectory() as folder:
            state = Path(folder) / 'state.json'
            state.write_text(json.dumps({'device': {'id': 'test-device'}, 'auth_key': 'test-password'}))
            argv = [str(SCRIPT), '--serial', '198.51.100.20:5555', '--host', '198.51.100.20', '--state', str(state)]
            if replace:
                argv.append('--replace-certificate')
            output = io.StringIO()
            with mock.patch.object(sys, 'argv', argv), mock.patch('subprocess.run', side_effect=run), mock.patch('http.client.HTTPSConnection', return_value=connection) as https, contextlib.redirect_stdout(output):
                error = None
                try:
                    runpy.run_path(str(SCRIPT), run_name='__main__')
                except SystemExit as exception:
                    error = exception
                command = shlex.split(calls[-1][-1])
                config = json.loads(command[command.index('--arg') + 1])
                return config, https.call_args, connection, output.getvalue(), error

    def test_preserves_existing_pin_without_network_discovery(self):
        config, address, _, output, error = self.exercise('Bundle[{certificate=' + PIN + '}]')
        self.assertIsNone(error)
        self.assertIsNone(address)
        self.assertEqual(config['certificate'], PIN)
        self.assertEqual(config['host'], '127.0.0.1')
        self.assertNotIn('test-password', output)

    def test_clean_install_discovers_remote_certificate_but_provisions_loopback(self):
        config, address, connection, _, error = self.exercise('Bundle[{certificate=}]')
        self.assertIsNone(error)
        self.assertEqual(address.args, ('198.51.100.20', 1926))
        self.assertEqual(config['host'], '127.0.0.1')
        self.assertEqual(config['certificate'], hashlib.sha256(CERTIFICATE).hexdigest())
        connection.close.assert_called_once()

    def test_explicit_replacement_refreshes_pin(self):
        config, address, connection, _, error = self.exercise('Bundle[{certificate=' + PIN + '}]', replace=True)
        self.assertIsNone(error)
        self.assertEqual(address.args, ('198.51.100.20', 1926))
        self.assertNotEqual(config['certificate'], PIN)
        self.assertEqual(config['certificate'], hashlib.sha256(CERTIFICATE).hexdigest())
        connection.close.assert_called_once()

    def test_rejected_configuration_reports_failure_without_credentials(self):
        _, _, _, output, error = self.exercise('Bundle[{certificate=' + PIN + '}]', success=False)
        self.assertIsInstance(error, SystemExit)
        self.assertNotIn('test-password', output + str(error))

if __name__ == '__main__':
    unittest.main()
