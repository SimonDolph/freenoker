var path = require('path');
var net = require('net');

/*global module:false*/
module.exports = function(grunt) {
  var Freenoker = {
    port: 65384,
    excludes: ['.ico', '.js', '.css']
  };

  var client = new net.Socket();

  client.connect(Freenoker.port, function() {
    grunt.log.writeln('Connected to freenoker server at port ' + Freenoker.port);
  });
  client.on('data', function(data) {
    Freenoker.res.end(data);
    grunt.log.writeln(new Date().getTime())
  });

  // Project configuration.
  grunt.initConfig({
    // Metadata.
    pkg: grunt.file.readJSON('package.json'),
    banner: '/*! <%= pkg.title || pkg.name %> - v<%= pkg.version %> - ' +
      '<%= grunt.template.today("yyyy-mm-dd") %>\n' +
      '<%= pkg.homepage ? "* " + pkg.homepage + "\\n" : "" %>' +
      '* Copyright (c) <%= grunt.template.today("yyyy") %> <%= pkg.author.name %>;' +
      ' Licensed <%= _.pluck(pkg.licenses, "type").join(", ") %> */\n',
    // Task configuration.
    connect: {
      options: {
        port: 8000,
        hostname: '*',
        livereload: 35728,
        middleware: function(connect, options, middlewares) {
          // inject a custom middleware into the array of default middlewares 
          middlewares.unshift(function(req, res, next) {
            grunt.log.writeln(new Date().getTime())
            var ext = path.extname(req.url);
            if (Freenoker.excludes.indexOf(ext) >= 0) {
              return next();
            } else {
              Freenoker.req = req;
              Freenoker.res = res;

              client.write(req.url);
            }
          });
 
          return middlewares;
        },
      },
      server: {
        options: {
          open: true,
          base: ['static']
        }
      }
    },
    concat: {
      options: {
        banner: '<%= banner %>',
        stripBanners: true
      },
      dist: {
        src: ['lib/<%= pkg.name %>.js'],
        dest: 'dist/<%= pkg.name %>.js'
      }
    },
    uglify: {
      options: {
        banner: '<%= banner %>'
      },
      dist: {
        src: '<%= concat.dist.dest %>',
        dest: 'dist/<%= pkg.name %>.min.js'
      }
    },
    jshint: {
      options: {
        curly: true,
        eqeqeq: true,
        immed: true,
        latedef: true,
        newcap: true,
        noarg: true,
        sub: true,
        undef: true,
        unused: true,
        boss: true,
        eqnull: true,
        browser: true,
        globals: {}
      },
      gruntfile: {
        src: 'Gruntfile.js'
      },
      lib_test: {
        src: ['lib/**/*.js', 'test/**/*.js']
      }
    },
    qunit: {
      files: ['test/**/*.html']
    },
    watch: {
      gruntfile: {
        files: '<%= jshint.gruntfile.src %>',
        options: {
          reload: true
        },
        tasks: ['jshint:gruntfile']
      },
      lib_test: {
        files: '<%= jshint.lib_test.src %>',
        tasks: ['jshint:lib_test', 'qunit']
      }
    }
  });

  // These plugins provide necessary tasks.
  grunt.loadNpmTasks('grunt-contrib-connect');
  grunt.loadNpmTasks('grunt-contrib-concat');
  grunt.loadNpmTasks('grunt-contrib-uglify');
  grunt.loadNpmTasks('grunt-contrib-qunit');
  grunt.loadNpmTasks('grunt-contrib-jshint');
  grunt.loadNpmTasks('grunt-contrib-watch');

  // Default task.
  // grunt.registerTask('default', ['jshint', 'qunit', 'concat', 'uglify']);
  grunt.registerTask('default', ['connect:server', 'watch']);

};
