#!perl
use strict;
use warnings;

use Path::Tiny qw(path);

my @cheatsheet;
my @body;

for my $line ( path($ARGV[0])->lines_utf8({ chomp => 1 }) ) {
  next unless $line =~ /^;;(.*)$/;
  my ( $expanded ) = $1;
  if ( $expanded =~ /^;\s?(\s*)(.*?)$/ ) {
    if ( (length $1) > 1 ) {
      $cheatsheet[-1] .= "\n" . $1 . $2;
    }
    else {
      push @cheatsheet, $2;
    }
    next;
  }
  $expanded =~ s/^ //;
  push @body, $expanded;
}
for my $line ( @body ) {
  printf "%s\n", $line;
  if ( $line =~ /^## Cheatsheet/ ) {
    print "\n";
    print "```clojure\n";
    for my $cheat ( sort @cheatsheet ) {
      printf "%s\n", $cheat;
    }
    print "```\n";
  }
}

