from lazyman.cli import main


def test_cli_main(capsys):
    result = main()

    captured = capsys.readouterr()

    assert result == 0
    assert "LazyMan is initialized." in captured.out
