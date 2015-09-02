var path = require('path'),
    net = require('net'),
    gulp = require('gulp'),
    connect = require('gulp-connect'),
    watch = require('gulp-watch'),
    less = require('gulp-less'),
    coffee = require('gulp-coffee'),
    gutil = require('gulp-util');

gulp.task('webserver', function() {
    var Freenoker = {
        port: 65384,
        excludes: ['.ico', '.js', '.css']
    };
    var client = new net.Socket();
    client.connect(Freenoker.port, function() {
        gutil.log('Connected to freenoker server at port ' + Freenoker.port);
    });
    client.on('data', function(data) {
        Freenoker.res.end(data);
    });
    connect.server({
        livereload: true,
        root: ['.', '.tmp'],
        middleware: function(connect, options) {
            return [function(req, res, next) {
                var ext = path.extname(req.url);
                if (Freenoker.excludes.indexOf(ext) >= 0) {
                    return next();
                } else {
                    Freenoker.req = req;
                    Freenoker.res = res;

                    client.write(req.url);
                }
            }];
        }
    });
});

gulp.task('livereload', function() {
    gulp.src(['.tmp/styles/*.css', '.tmp/scripts/*.js', 'index.html'])
        .pipe(watch(['.tmp/**', 'index.html']))
        .pipe(connect.reload());
});

gulp.task('less', function() {
    gulp.src('styles/main.less')
        .pipe(less())
        .pipe(gulp.dest('.tmp/styles'));
});

gulp.task('coffee', function() {
    gulp.src('scripts/*.coffee')
        .pipe(coffee().on('error', function(err) {
            this.emit('end');
        }))
        .pipe(gulp.dest('.tmp/scripts'));
});

gulp.task('watch', function() {
    gulp.watch('styles/*.less', ['less']);
    gulp.watch('scripts/*.coffee', ['coffee']);
})

gulp.task('default', ['less', 'coffee', 'webserver', 'livereload', 'watch']);