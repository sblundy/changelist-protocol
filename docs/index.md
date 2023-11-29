# Changelist Protocol Documentation

## Built-in Server Rest API

[OpenAPI Schema](./openapi)

## URL Scheme Usage

The base URL is `jetbrains://<IDE ID>/changelist/<target>`

### Known IDE IDs

Known identifiers for each JetBrains IDE. These are the first element in the URL path.
Those marked as _unknown_ are have not been checked, but are probably the IDE name in lower-case.
Those that are _none_ have been attempted and not responded to any probable IDs.

| IDE            | Identifier  |
|----------------|-------------|
| Aqua           | _unknown_   |
| CLion          | `clion`     |
| ~~DataGrip~~   | _none_      |
| DataSpell      | _unknown_   |
| Fleet          | _unknown_   |
| ~~Gateway~~    | _none_      |
| GoLand         | `goland`    |
| IntelliJ IDEA  | `idea`      |
| PhpStorm       | `php-storm` |
| PyCharm        | `pycharm`   |
| Rider          | `rider`     |
| RubyMine       | `rubymine`  |
| ~~RustRover~~  | _none_      |
| WebStorm       | `webstorm`  |

### Available Targets

#### New Changelist

**Example**
```shell
open jetbrains://idea/changelist/add?project=changelist-protocol&name=Actual%20Changelist&comment=Test%20comment
```

| Parameter | Required | Type    | Description                                                   |
|-----------|----------|---------|---------------------------------------------------------------|
| `project` | Yes      | string  | Project to create the changelist in                           |
| `name`    | Yes      | string  | Changelist name                                               |
| `comment` | No       | string  | Changelist comment                                            |
| `active`  | No       | boolean | Whether to make new changelist the active one (default: true) |

#### Set Active Changelist

**Example**
```shell
open jetbrains://pycharm/changelist/activate?project=changelist-protocol&name=New%20changelist
```

| Parameter | Required                        | Type    | Description                     |
|-----------|---------------------------------|---------|---------------------------------|
| `project` | Yes                             | string  | Changelist's project            |
| `name`    | Yes, unless `default` is `true` | string  | Changelist name                 |
| `default` | No                              | boolean | Active the default changelist   |

#### Update Changelist

**Example**
```shell
open jetbrains://pycharm/changelist/update?project=changelist-protocol&name=New%20changelist&comment=Editted%20comment
```

| Parameter  | Required | Type    | Description                               |
|------------|----------|---------|-------------------------------------------|
| `project`  | Yes      | string  | Changelist's project                      |
| `name`     | Yes      | string  | Changelist name                           |
| `new-name` | No       | string  | Rename Changelist to this name            |
| `active`   | No       | boolean | Whether to make changelist the active one |
| `comment`  | No       | string  | Changelist comment                        |

#### Delete Changelist

**Example**
```shell
open jetbrains://goland/changelist/remove?project=changelist-protocol&name=Actual%20Changelist
```

| Parameter | Required | Type    | Description          |
|-----------|----------|---------|----------------------|
| `project` | Yes      | string  | Changelist's project |
| `name`    | Yes      | string  | Changelist name      |
